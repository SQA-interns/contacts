package si.contactgroups.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import si.contactgroups.domain.Contact;
import si.contactgroups.domain.TrashContactView;
import si.contactgroups.domain.TrashPeriod;
import si.contactgroups.persistence.ContactRepository;

/** Delete a Contact into Trash, list, restore and delete forever (US-012, US-013, BR-04). */
@Service
public class TrashService {

  private static final Logger LOG = LoggerFactory.getLogger(TrashService.class);

  private final ContactRepository contacts;
  private final ContactService contactService;
  private final HistoryWriter history;
  private final Clock clock;
  private final TransactionTemplate tx;

  public TrashService(
      ContactRepository contacts,
      ContactService contactService,
      HistoryWriter history,
      Clock clock,
      PlatformTransactionManager transactions) {
    this.tx = new TransactionTemplate(transactions);
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

  /**
   * Deletes a Contact in Trash for good, before the 30 days are over too, and records it (R-013-01,
   * R-013-06).
   *
   * @throws ContactNotFoundException when it is not in Trash of this Group (also for the second of
   *     two requests at once, R-013-05, EX-013-04)
   */
  @Transactional
  public void deleteForever(UUID groupId, UUID userId, UUID contactId) {
    purge(groupId, userId, contactId);
  }

  /**
   * Deletes every Contact that has been in Trash for 30 days, each in its own transaction so that
   * one failure does not stop the others (R-013-04, ADR-09).
   *
   * @return the number of Contacts deleted
   */
  public int purgeExpired() {
    int deleted = 0;
    for (var expired : contacts.listExpired(TrashPeriod.cutoff(now()))) {
      try {
        if (tx.execute(s -> purge(expired.groupId(), null, expired.contactId()))) {
          deleted++;
        }
      } catch (RuntimeException e) {
        LOG.warn("Trash purge of one Contact failed: {}", e.getClass().getSimpleName());
      }
    }
    return deleted;
  }

  private boolean purge(UUID groupId, UUID userId, UUID contactId) {
    if (!contacts.deleteFromTrash(groupId, contactId)) {
      throw new ContactNotFoundException();
    }
    history.contactDeletedForever(groupId, userId, contactId, now());
    return true;
  }

  private Instant now() {
    return clock.instant().truncatedTo(ChronoUnit.MICROS);
  }
}
