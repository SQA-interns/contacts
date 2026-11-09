package si.contactgroups.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import si.contactgroups.domain.Contact;
import si.contactgroups.domain.TrashContactView;
import si.contactgroups.domain.TrashPeriod;
import si.contactgroups.persistence.ContactRepository;

/** Delete a Contact into Trash, list the Trash and restore from it (US-012, BR-04). */
@Service
public class TrashService {

  private final ContactRepository contacts;
  private final ContactService contactService;
  private final HistoryWriter history;
  private final Clock clock;

  public TrashService(
      ContactRepository contacts,
      ContactService contactService,
      HistoryWriter history,
      Clock clock) {
    this.contacts = contacts;
    this.contactService = contactService;
    this.history = history;
    this.clock = clock;
  }

  /**
   * Moves the Contact to Trash and records it; a Contact already in Trash stays as it is and
   * nothing is recorded (R-012-09).
   *
   * @throws ContactNotFoundException when the Group has no such Contact
   */
  @Transactional
  public void moveToTrash(UUID groupId, UUID userId, UUID contactId) {
    Instant now = now();
    if (contacts.moveToTrash(groupId, contactId, now)) {
      history.contactDeleted(groupId, userId, contactId, now);
    } else if (!contacts.existsIncludingTrash(groupId, contactId)) {
      throw new ContactNotFoundException();
    }
  }

  /** The Contacts in Trash of the Group with the days left (R-012-03). */
  public List<TrashContactView> list(UUID groupId) {
    Instant now = now();
    return contacts.listTrash(groupId, TrashPeriod.cutoff(now)).stream()
        .map(c -> TrashContactView.of(c, now))
        .toList();
  }

  /**
   * Returns the Contact to the Group with all it had and records it (R-012-04, R-012-07).
   *
   * @throws ContactNotFoundException when it is not in Trash or has been for 30 days (R-012-05)
   */
  @Transactional
  public Contact restore(UUID groupId, UUID userId, UUID contactId) {
    Instant now = now();
    if (!contacts.restoreFromTrash(groupId, contactId, TrashPeriod.cutoff(now))) {
      throw new ContactNotFoundException();
    }
    history.contactRestored(groupId, userId, contactId, now);
    return contactService.get(groupId, contactId);
  }

  private Instant now() {
    return clock.instant().truncatedTo(ChronoUnit.MICROS);
  }
}
