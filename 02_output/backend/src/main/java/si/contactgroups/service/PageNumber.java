package si.contactgroups.service;

/**
 * The {@code page} query parameter of every list (contract {@code PageParam}: integer, 1 or more).
 */
public final class PageNumber {

  private PageNumber() {}

  /**
   * @param page the text of the parameter; {@code null} means the first page
   * @throws InvalidQueryException when it is not a whole number of at least 1 (and at most 9
   *     digits)
   */
  public static int parse(String page) {
    if (page == null) {
      return 1;
    }
    int number = page.matches("[0-9]{1,9}") ? Integer.parseInt(page) : 0;
    if (number < 1) {
      throw new InvalidQueryException("Page must be a whole number of at least 1");
    }
    return number;
  }
}
