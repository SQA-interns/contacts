package si.contactgroups.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import si.contactgroups.domain.AttributeChanges;
import si.contactgroups.domain.AttributePair;
import si.contactgroups.domain.Contact;
import si.contactgroups.domain.ContactData;
import si.contactgroups.domain.StoredContact;
import si.contactgroups.domain.TagChanges;
import si.contactgroups.domain.VersionContent;
import si.contactgroups.error.ApiError.FieldError;
import si.contactgroups.persistence.ContactRepository;
import si.contactgroups.persistence.VersionRepository;

/** Create, read and edit a Contact (US-003, BR-06, BR-07). */
@Service
public class ContactService {

  private final ContactRepository contacts;
  private final VersionRepository versions;
  private final HistoryWriter history;
  private final ContactValidator validator;
  private final AttributeService attributes;
  private final TagService tags;
  private final Clock clock;

  public ContactService(
      ContactRepository contacts,
      VersionRepository versions,
      HistoryWriter history,
      ContactValidator validator,
      AttributeService attributes,
      TagService tags,
      Clock clock) {
    this.contacts = contacts;
    this.versions = versions;
    this.history = history;
    this.validator = validator;
    this.attributes = attributes;
    this.tags = tags;
    this.clock = clock;
  }

  /**
   * Stores a new Contact with version number 1 (R-003-01).
   *
   * @param pairs the Attributes; {@code null} means none
   * @param tagNames the Tags; {@code null} means none
   * @throws ValidationException when BR-06 or R-004-02..04 is broken
   */
  @Transactional
  public Contact create(
      UUID groupId,
      UUID userId,
      ContactData input,
      List<AttributePair> pairs,
      List<String> tagNames) {
    List<FieldError> errors = new ArrayList<>();
    ContactData data = check(input, errors);
    List<AttributePair> cleaned = attributes.clean(pairs, errors);
    List<String> cleanedTags = tags.clean(tagNames, errors);
    failOn(errors);
    Instant now = now();
    UUID id = contacts.insert(groupId, userId, data, now);
    history.contactCreated(groupId, userId, id, data, now);
    attributes.replace(groupId, userId, id, List.of(), cleaned, now);
    tags.replace(groupId, userId, id, List.of(), cleanedTags, now);
    versions.insertSnapshot(groupId, id, 1, userId, now);
    return load(groupId, id);
  }

  /**
   * @throws ContactNotFoundException when the Group has no such Contact (BR-01)
   */
  public Contact get(UUID groupId, UUID contactId) {
    return load(groupId, contactId);
  }

  /**
   * Saves changes made on top of {@code version} (R-003-06, R-003-07, R-003-11).
   *
   * @throws VersionMissingException when {@code version} is {@code null}
   * @param pairs the Attributes; {@code null} keeps the stored ones (R-004-07: any change is a
   *     save)
   * @param tagNames the Tags; {@code null} keeps the stored ones (R-005-08: any change is a save)
   * @throws ValidationException when BR-06, R-004-02..04 or R-005-02, R-005-04 is broken
   * @throws ContactNotFoundException when the Group has no such Contact
   * @throws ContactChangedException when {@code version} is not the current one (BR-07)
   */
  @Transactional
  public Contact update(
      UUID groupId,
      UUID userId,
      UUID contactId,
      Integer version,
      ContactData input,
      List<AttributePair> pairs,
      List<String> tagNames) {
    return save(groupId, userId, contactId, version, input, pairs, tagNames, null);
  }

  /**
   * Saves the content of an earlier Version as a new Version (R-023-01): the same checks and the
   * same save as {@link #update}, but the History records "restored version {@code restoredFrom}"
   * instead of an edit (R-023-02). The Tags and Attributes are those of the Version; a Tag that is
   * gone is created again (R-023-05).
   *
   * @throws VersionMissingException when {@code version} is {@code null}
   * @throws ValidationException when the content breaks BR-06 (R-023-03)
   * @throws ContactChangedException when {@code version} is not the current one (R-023-04)
   */
  @Transactional
  public Contact restore(
      UUID groupId,
      UUID userId,
      UUID contactId,
      Integer version,
      int restoredFrom,
      VersionContent content) {
    return save(
        groupId,
        userId,
        contactId,
        version,
        content.data(),
        content.attributes(),
        content.tags(),
        restoredFrom);
  }

  private Contact save(
      UUID groupId,
      UUID userId,
      UUID contactId,
      Integer version,
      ContactData input,
      List<AttributePair> pairs,
      List<String> tagNames,
      Integer restoredFrom) {
    if (version == null) {
      throw new VersionMissingException();
    }
    Cleaned cleaned = clean(input, pairs, tagNames);
    StoredContact stored = current(groupId, contactId, version);
    List<AttributePair> before = attributes.list(groupId, contactId);
    List<AttributePair> after = orKeep(pairs, cleaned.pairs(), before);
    List<String> tagsBefore = tags.list(groupId, contactId);
    List<String> tagsAfter = orKeep(tagNames, cleaned.tags(), tagsBefore);
    if (stored.data().equals(cleaned.data()) && unchanged(before, after, tagsBefore, tagsAfter)) {
      return view(stored, before, tagsBefore);
    }
    Instant now = now();
    if (!contacts.update(groupId, contactId, version, cleaned.data(), now)) {
      throw contacts.isInTrash(groupId, contactId)
          ? new ContactInTrashException()
          : new ContactChangedException(load(groupId, contactId));
    }
    if (restoredFrom == null) {
      history.contactEdited(groupId, userId, contactId, stored.data(), cleaned.data(), now);
    }
    attributes.replace(groupId, userId, contactId, before, after, now);
    tags.replace(groupId, userId, contactId, tagsBefore, tagsAfter, now);
    if (restoredFrom != null) {
      history.versionRestored(
          groupId, userId, contactId, restoredFrom, stored.data(), cleaned.data(), now);
    }
    versions.insertSnapshot(groupId, contactId, version + 1, userId, now);
    return load(groupId, contactId);
  }

  /**
   * The fields, Attributes and Tags after the checks of BR-06, R-004-02..04 and R-005-02, R-005-04.
   */
  private record Cleaned(ContactData data, List<AttributePair> pairs, List<String> tags) {}

  private Cleaned clean(ContactData input, List<AttributePair> pairs, List<String> tagNames) {
    List<FieldError> errors = new ArrayList<>();
    ContactData data = check(input, errors);
    List<AttributePair> cleanedPairs = attributes.clean(pairs, errors);
    List<String> cleanedTags = tags.clean(tagNames, errors);
    failOn(errors);
    return new Cleaned(data, cleanedPairs, cleanedTags);
  }

  /** The stored Contact, when {@code version} is its current number (BR-07). */
  private StoredContact current(UUID groupId, UUID contactId, int version) {
    StoredContact stored =
        contacts.find(groupId, contactId).orElseThrow(() -> missing(groupId, contactId));
    if (stored.version() != version) {
      throw new ContactChangedException(load(groupId, contactId));
    }
    return stored;
  }

  /** A Contact in Trash may not be changed (R-012-06); any other missing one is not found. */
  private RuntimeException missing(UUID groupId, UUID contactId) {
    return contacts.isInTrash(groupId, contactId)
        ? new ContactInTrashException()
        : new ContactNotFoundException();
  }

  private ContactData check(ContactData input, List<FieldError> errors) {
    try {
      return validator.validate(input);
    } catch (ValidationException e) {
      errors.addAll(e.getFieldErrors());
      return input;
    }
  }

  private static void failOn(List<FieldError> errors) {
    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
  }

  private static <T> List<T> orKeep(List<?> sent, List<T> cleaned, List<T> stored) {
    return sent == null ? stored : cleaned;
  }

  private static boolean unchanged(
      List<AttributePair> before,
      List<AttributePair> after,
      List<String> tagsBefore,
      List<String> tagsAfter) {
    return AttributeChanges.between(before, after).isEmpty()
        && TagChanges.between(tagsBefore, tagsAfter).isEmpty();
  }

  private Contact load(UUID groupId, UUID contactId) {
    StoredContact row =
        contacts.find(groupId, contactId).orElseThrow(ContactNotFoundException::new);
    return view(row, attributes.list(groupId, contactId), tags.list(groupId, contactId));
  }

  private static Contact view(StoredContact row, List<AttributePair> pairs, List<String> tagNames) {
    return new Contact(
        row.id(), row.version(), row.data(), tagNames, pairs, row.createdAt(), row.updatedAt());
  }

  private Instant now() {
    return clock.instant().truncatedTo(ChronoUnit.MICROS);
  }
}
