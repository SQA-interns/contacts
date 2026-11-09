package si.contactgroups.service;

/** The Selection has more Contacts than one Export file may hold (R-011-05, 422). */
public class ExportTooManyException extends RuntimeException {

  public ExportTooManyException(int limit) {
    super("The Selection has more than " + limit + " Contacts; narrow it");
  }
}
