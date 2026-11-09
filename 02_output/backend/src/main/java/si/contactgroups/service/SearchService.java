package si.contactgroups.service;

import java.util.UUID;
import org.springframework.stereotype.Service;
import si.contactgroups.domain.ContactPage;
import si.contactgroups.persistence.ContactRepository;

/** The Contact list of a Group: sorted and paged, never Trash (US-007, BR-01, BR-04). */
@Service
public class SearchService {

  /** Page size of the Contact list (R-007-01). */
  public static final int PAGE_SIZE = 25;

  private final ContactRepository contacts;

  public SearchService(ContactRepository contacts) {
    this.contacts = contacts;
  }

  /**
   * @param page the 1-based page number as sent; {@code null} means 1
   * @param sort a column of the list; {@code null} means last name
   * @param dir {@code asc} or {@code desc}; {@code null} means {@code asc}
   * @throws InvalidQueryException when a parameter is not allowed by the contract
   */
  public ContactPage list(UUID groupId, String page, String sort, String dir) {
    int number = pageNumber(page);
    boolean descending = descending(dir);
    String column = sort == null ? "lastName" : sort;
    if (!ContactRepository.SORT_COLUMNS.contains(column)) {
      throw new InvalidQueryException("Unknown sort column");
    }
    int total = contacts.count(groupId);
    long offset = (long) (number - 1) * PAGE_SIZE;
    var items =
        offset >= total
            ? java.util.List.<si.contactgroups.domain.ContactListItem>of()
            : contacts.page(groupId, column, descending, (int) offset, PAGE_SIZE);
    return new ContactPage(items, number, PAGE_SIZE, total);
  }

  private static int pageNumber(String page) {
    if (page == null) {
      return 1;
    }
    try {
      int number = Integer.parseInt(page);
      if (number >= 1) {
        return number;
      }
    } catch (NumberFormatException e) {
      // falls through to the refusal below
    }
    throw new InvalidQueryException("Page must be a whole number of at least 1");
  }

  private static boolean descending(String dir) {
    if (dir == null || "asc".equals(dir)) {
      return false;
    }
    if ("desc".equals(dir)) {
      return true;
    }
    throw new InvalidQueryException("Direction must be asc or desc");
  }
}
