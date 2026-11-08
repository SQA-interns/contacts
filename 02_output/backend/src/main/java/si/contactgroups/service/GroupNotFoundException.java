package si.contactgroups.service;

/**
 * The Group of the request does not exist, is in Trash, is not a Group id, or the caller is not a
 * Member of it. The answer never tells these apart (R-018-02, EX-018-01, EX-018-02, BR-01).
 */
public class GroupNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public GroupNotFoundException() {
    super("Group not found");
  }
}
