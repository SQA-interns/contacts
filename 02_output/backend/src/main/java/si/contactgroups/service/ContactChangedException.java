package si.contactgroups.service;

import si.contactgroups.domain.Contact;

/** A save with a version number that is not the current one (BR-07, R-003-07). */
public class ContactChangedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final transient Contact current;

  public ContactChangedException(Contact current) {
    super("This contact was changed by someone else");
    this.current = current;
  }

  /** The Contact as it is stored now. */
  public Contact getCurrent() {
    return current;
  }
}
