package si.contactgroups.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import si.contactgroups.domain.AttributePair;
import si.contactgroups.domain.Contact;
import si.contactgroups.domain.ContactData;
import si.contactgroups.domain.StoredContact;
import si.contactgroups.error.ApiError.FieldError;
import si.contactgroups.persistence.ContactRepository;

/**
 * Merges duplicate Contacts into the Survivor (US-026). Everything happens in one transaction: the
 * Survivor is saved as any save (BR-06, BR-07, new version, Version, History), the others go to
 * Trash (BR-04) and one History entry records the merge (BR-03); a refusal leaves nothing behind.
 */
@Service
public class MergeService {

  private final ContactRepository contacts;
  private final ContactService contactService;
  private final AttributeService attributes;
  private final TagService tags;
  private final HistoryWriter history;
  private final Clock clock;

  public MergeService(
      ContactRepository contacts,
      ContactService contactService,
      AttributeService attributes,
      TagService tags,
      HistoryWriter history,
      Clock clock) {
    this.contacts = contacts;
    this.contactService = contactService;
    this.attributes = attributes;
    this.tags = tags;
    this.history = history;
    this.clock = clock;
  }

  /**
   * Merges the Contacts of the command and returns the Survivor.
   *
   * @throws MergeSelectionException when fewer than two different Contacts are named or the
   *     Survivor is not one of them (R-026-09)
   * @throws ContactNotFoundException when a Contact is not in this Group
   * @throws ContactInTrashException when a Contact is in Trash (EX-026-07)
   * @throws ContactChangedException when a Contact has another version than the user saw (R-026-08)
   * @throws MergeValueException when a chosen value is no value of a merged Contact (D-19 d)
   * @throws ValidationException when the result breaks BR-06 or a limit (R-026-06)
   */
  @Transactional
  public Contact merge(UUID groupId, UUID userId, MergeCommand command) {
    List<MergeCommand.Target> targets = distinctTargets(command);
    List<Source> sources = read(groupId, command.survivorId(), targets);
    List<MergeSource> values = sources.stream().map(Source::values).toList();
    ContactData fields = MergeValues.chooseFields(orEmpty(command.fields()), values);
    List<AttributePair> pairs = MergeValues.combineAttributes(chosenAttributes(command), values);
    List<String> mergedTags = MergeValues.combineTags(values);

    Source survivor = sources.get(0);
    Contact saved =
        contactService.update(
            groupId, userId, survivor.id(), survivor.version(), fields, pairs, mergedTags);
    Instant now = now();
    List<UUID> goneIds = new ArrayList<>();
    for (Source other : sources.subList(1, sources.size())) {
      if (!contacts.moveToTrashAtVersion(groupId, other.id(), other.version(), now)) {
        throw changedOrTrashed(groupId, other.id());
      }
      history.contactDeleted(groupId, userId, other.id(), now);
      goneIds.add(other.id());
    }
    history.contactsMerged(
        groupId,
        userId,
        survivor.id(),
        goneIds,
        saved.data(),
        saved.attributes(),
        saved.tags(),
        now);
    return contactService.get(groupId, survivor.id());
  }

  /** The named Contacts without the Survivor's duplicates; at least two (R-026-09, EX-026-06). */
  private static List<MergeCommand.Target> distinctTargets(MergeCommand command) {
    if (command.contacts() == null || command.survivorId() == null) {
      throw new MergeSelectionException();
    }
    command.contacts().forEach(MergeService::requireComplete);
    Map<UUID, MergeCommand.Target> byId = new LinkedHashMap<>();
    command.contacts().forEach(target -> byId.putIfAbsent(target.id(), target));
    boolean selection =
        byId.size() >= 2
            && byId.size() == command.contacts().size()
            && byId.containsKey(command.survivorId());
    if (!selection) {
      throw new MergeSelectionException();
    }
    return List.copyOf(byId.values());
  }

  private static void requireComplete(MergeCommand.Target target) {
    if (target == null || target.id() == null) {
      throw new MergeSelectionException();
    }
    if (target.version() == null) {
      throw new VersionMissingException();
    }
  }

  /** What the service read of one merged Contact. */
  private record Source(StoredContact stored, MergeSource values) {
    UUID id() {
      return stored.id();
    }

    int version() {
      return stored.version();
    }
  }

  /** The merged Contacts, the Survivor first and then the others, oldest first (R-026-02). */
  private List<Source> read(UUID groupId, UUID survivorId, List<MergeCommand.Target> targets) {
    List<Source> sources = new ArrayList<>();
    for (MergeCommand.Target target : targets) {
      StoredContact stored =
          contacts.find(groupId, target.id()).orElseThrow(() -> missing(groupId, target.id()));
      if (stored.version() != target.version()) {
        throw new ContactChangedException(contactService.get(groupId, target.id()));
      }
      sources.add(
          new Source(
              stored,
              new MergeSource(
                  stored.id(),
                  stored.data(),
                  attributes.list(groupId, stored.id()),
                  tags.list(groupId, stored.id()))));
    }
    sources.sort(
        Comparator.comparing((Source s) -> !s.id().equals(survivorId))
            .thenComparing(s -> s.stored().createdAt())
            .thenComparing(Source::id));
    return sources;
  }

  private RuntimeException missing(UUID groupId, UUID contactId) {
    return contacts.isInTrash(groupId, contactId)
        ? new ContactInTrashException()
        : new ContactNotFoundException();
  }

  private RuntimeException changedOrTrashed(UUID groupId, UUID contactId) {
    return contacts.find(groupId, contactId).isPresent()
        ? new ContactChangedException(contactService.get(groupId, contactId))
        : missing(groupId, contactId);
  }

  /**
   * The chosen Attribute values of the command.
   *
   * @throws ValidationException when a choice is null or has no key (SB-01)
   */
  private static List<AttributePair> chosenAttributes(MergeCommand command) {
    List<AttributePair> chosen = command.attributes() == null ? List.of() : command.attributes();
    if (chosen.stream().anyMatch(pair -> pair == null || pair.key() == null)) {
      throw new ValidationException(List.of(new FieldError("attributes", "validation.required")));
    }
    return chosen;
  }

  private static ContactData orEmpty(ContactData fields) {
    return fields == null
        ? new ContactData(null, null, null, null, null, null, null, null, null, null, null)
        : fields;
  }

  private Instant now() {
    return clock.instant().truncatedTo(ChronoUnit.MICROS);
  }
}
