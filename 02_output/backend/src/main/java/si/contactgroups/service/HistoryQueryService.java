package si.contactgroups.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import si.contactgroups.domain.ActivityFilter;
import si.contactgroups.domain.ActivityRange;
import si.contactgroups.domain.HistoryAction;
import si.contactgroups.domain.HistoryEntry;
import si.contactgroups.persistence.ContactRepository;
import si.contactgroups.persistence.HistoryRepository;

/** Reads the History of a Contact (R-016-03) and of a Group (R-017-01). */
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

  /**
   * One page of a Contact's History, newest first, with the History of the Contacts merged into it
   * (R-026-07); the Contact may be in Trash (R-016-08).
   */
  public Page contactHistory(UUID groupId, UUID contactId, int page) {
    if (!contacts.existsIncludingTrash(groupId, contactId)) {
      throw new ContactNotFoundException();
    }
    List<UUID> related = history.contactAndMergedIds(groupId, contactId);
    int number = Math.max(page, 1);
    List<HistoryEntry> items =
        history.pageOfContacts(groupId, related, PAGE_SIZE, (number - 1) * PAGE_SIZE);
    return new Page(items, number, PAGE_SIZE, history.countOfContacts(groupId, related));
  }

  /**
   * One page of the Group activity list, newest first (R-017-01..07). Filters are text as the
   * request carries them; {@code null} or blank means no filter.
   */
  public Page groupActivity(
      UUID groupId, String userId, String action, String from, String to, int page) {
    ActivityRange range = new ActivityRange(parseDay(from), parseDay(to));
    ActivityFilter filter =
        new ActivityFilter(
            parseUser(userId), parseAction(action), range.fromInclusive(), range.toExclusive());
    if (range.isInverted()) {
      throw new ActivityRangeException();
    }
    int number = Math.max(page, 1);
    List<HistoryEntry> items =
        history.pageOfGroup(groupId, filter, PAGE_SIZE, (number - 1) * PAGE_SIZE);
    return new Page(items, number, PAGE_SIZE, history.countOfGroup(groupId, filter));
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private static LocalDate parseDay(String value) {
    try {
      return blank(value) ? null : LocalDate.parse(value);
    } catch (DateTimeParseException e) {
      throw new InvalidQueryException("date must be yyyy-MM-dd");
    }
  }

  private static UUID parseUser(String value) {
    try {
      return blank(value) ? null : UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      throw new InvalidQueryException("userId must be a UUID");
    }
  }

  private static HistoryAction parseAction(String value) {
    try {
      return blank(value) ? null : HistoryAction.valueOf(value);
    } catch (IllegalArgumentException e) {
      throw new InvalidQueryException("unknown action");
    }
  }

  /** Contract schema {@code HistoryPage}. */
  public record Page(List<HistoryEntry> items, int page, int pageSize, long total) {}
}
