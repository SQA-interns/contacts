package si.contactgroups.service;

/** A change to a Contact that is in Trash (R-012-06, EX-012-05). */
public class ContactInTrashException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ContactInTrashException() {
    super("Contact is in Trash");
  }
}
