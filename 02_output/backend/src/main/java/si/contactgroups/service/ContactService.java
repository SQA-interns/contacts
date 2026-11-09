package si.contactgroups.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import si.contactgroups.domain.Contact;
import si.contactgroups.domain.ContactData;
import si.contactgroups.domain.StoredContact;
import si.contactgroups.persistence.ContactRepository;
import si.contactgroups.persistence.VersionRepository;

/** Create, read and edit a Contact (US-003, BR-06, BR-07). */
@Service
public class ContactService {

  private final ContactRepository contacts;
  private final VersionRepository versions;
  private final HistoryWriter history;
  private final ContactValidator validator;
  private final Clock clock;

  public ContactService(
      ContactRepository contacts,
      VersionRepository versions,
      HistoryWriter history,
      ContactValidator validator,
      Clock clock) {
    this.contacts = contacts;
    this.versions = versions;
    this.history = history;
    this.validator = validator;
    this.clock = clock;
  }

  /**
   * Stores a new Contact with version number 1 (R-003-01).
   *
   * @throws ValidationException when BR-06 is broken
   */
  @Transactional
  public Contact create(UUID groupId, UUID userId, ContactData input) {
    ContactData data = validator.validate(input);
    Instant now = now();
    UUID id = contacts.insert(groupId, userId, data, now);
    history.contactCreated(groupId, userId, id, data, now);
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
   * @throws ValidationException when BR-06 is broken
   * @throws ContactNotFoundException when the Group has no such Contact
   * @throws ContactChangedException when {@code version} is not the current one (BR-07)
   */
  @Transactional
  public Contact update(
      UUID groupId, UUID userId, UUID contactId, Integer version, ContactData input) {
    if (version == null) {
      throw new VersionMissingException();
    }
    ContactData data = validator.validate(input);
    StoredContact stored =
        contacts.find(groupId, contactId).orElseThrow(ContactNotFoundException::new);
    if (stored.version() != version) {
      throw new ContactChangedException(view(stored));
    }
    if (stored.data().equals(data)) {
      return view(stored);
    }
    Instant now = now();
    if (!contacts.update(groupId, contactId, version, data, now)) {
      throw new ContactChangedException(load(groupId, contactId));
    }
    history.contactEdited(groupId, userId, contactId, stored.data(), data, now);
    versions.insertSnapshot(groupId, contactId, version + 1, userId, now);
    return load(groupId, contactId);
  }

  private Contact load(UUID groupId, UUID contactId) {
    return view(contacts.find(groupId, contactId).orElseThrow(ContactNotFoundException::new));
  }

  private static Contact view(StoredContact row) {
    return new Contact(
        row.id(),
        row.version(),
        row.data(),
        List.of(),
        List.of(),
        row.createdAt(),
        row.updatedAt());
  }

  private Instant now() {
    return clock.instant().truncatedTo(ChronoUnit.MICROS);
  }
}
