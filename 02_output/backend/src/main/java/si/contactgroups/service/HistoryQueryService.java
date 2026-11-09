package si.contactgroups.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import si.contactgroups.domain.HistoryEntry;
import si.contactgroups.persistence.ContactRepository;
import si.contactgroups.persistence.HistoryRepository;

/** Reads the History of a Contact (R-016-03). */
@Service
public class HistoryQueryService {

  /** Entries per page (contract {@code HistoryPage}). */
  public static final int PAGE_SIZE = 25;

  private final HistoryRepository history;
  private final ContactRepository contacts;

  public HistoryQueryService(HistoryRepository history, ContactRepository contacts) {
    this.history = history;
    this.contacts = contacts;
  }

  /** One page of a Contact's History, newest first; the Contact may be in Trash (R-016-08). */
  public Page contactHistory(UUID groupId, UUID contactId, int page) {
    if (!contacts.existsIncludingTrash(groupId, contactId)) {
      throw new ContactNotFoundException();
    }
    int number = Math.max(page, 1);
    List<HistoryEntry> items =
        history.pageOfContact(groupId, contactId, PAGE_SIZE, (number - 1) * PAGE_SIZE);
    return new Page(items, number, PAGE_SIZE, history.countOfContact(groupId, contactId));
  }

  /** Contract schema {@code HistoryPage}. */
  public record Page(List<HistoryEntry> items, int page, int pageSize, long total) {}
}
