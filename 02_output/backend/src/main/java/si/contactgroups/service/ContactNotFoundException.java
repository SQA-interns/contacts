package si.contactgroups.service;

/**
 * The Contact does not exist in this Group, is in Trash, or the id is not a Contact id; the answer
 * never tells these apart (BR-01, R-018-05).
 */
public class ContactNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ContactNotFoundException() {
    super("Contact not found");
  }
}
