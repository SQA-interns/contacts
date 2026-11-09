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
  private final Clock clock;

  public ContactService(
      ContactRepository contacts,
      VersionRepository versions,
      HistoryWriter history,
      ContactValidator validator,
      AttributeService attributes,
      Clock clock) {
    this.contacts = contacts;
    this.versions = versions;
    this.history = history;
    this.validator = validator;
    this.attributes = attributes;
    this.clock = clock;
  }

  /**
   * Stores a new Contact with version number 1 (R-003-01).
   *
   * @param pairs the Attributes; {@code null} means none
   * @throws ValidationException when BR-06 or R-004-02..04 is broken
   */
  @Transactional
  public Contact create(UUID groupId, UUID userId, ContactData input, List<AttributePair> pairs) {
    List<FieldError> errors = new ArrayList<>();
    ContactData data = check(input, errors);
    List<AttributePair> cleaned = attributes.clean(pairs, errors);
    failOn(errors);
    Instant now = now();
    UUID id = contacts.insert(groupId, userId, data, now);
    history.contactCreated(groupId, userId, id, data, now);
    attributes.replace(groupId, userId, id, List.of(), cleaned, now);
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
   * @throws ValidationException when BR-06 or R-004-02..04 is broken
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
      List<AttributePair> pairs) {
    if (version == null) {
      throw new VersionMissingException();
    }
    List<FieldError> errors = new ArrayList<>();
    ContactData data = check(input, errors);
    List<AttributePair> cleaned = attributes.clean(pairs, errors);
    failOn(errors);
    StoredContact stored =
        contacts.find(groupId, contactId).orElseThrow(ContactNotFoundException::new);
    if (stored.version() != version) {
      throw new ContactChangedException(load(groupId, contactId));
    }
    List<AttributePair> before = attributes.list(groupId, contactId);
    List<AttributePair> after = pairs == null ? before : cleaned;
    if (stored.data().equals(data) && sameAttributes(before, after)) {
      return view(stored, before);
    }
    Instant now = now();
    if (!contacts.update(groupId, contactId, version, data, now)) {
      throw new ContactChangedException(load(groupId, contactId));
    }
    history.contactEdited(groupId, userId, contactId, stored.data(), data, now);
    attributes.replace(groupId, userId, contactId, before, after, now);
    versions.insertSnapshot(groupId, contactId, version + 1, userId, now);
    return load(groupId, contactId);
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

  private static boolean sameAttributes(List<AttributePair> before, List<AttributePair> after) {
    return AttributeChanges.between(before, after).isEmpty();
  }

  private Contact load(UUID groupId, UUID contactId) {
    StoredContact row =
        contacts.find(groupId, contactId).orElseThrow(ContactNotFoundException::new);
    return view(row, attributes.list(groupId, contactId));
  }

  private static Contact view(StoredContact row, List<AttributePair> pairs) {
    return new Contact(
        row.id(), row.version(), row.data(), List.of(), pairs, row.createdAt(), row.updatedAt());
  }

  private Instant now() {
    return clock.instant().truncatedTo(ChronoUnit.MICROS);
  }
}
