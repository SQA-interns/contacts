package si.contactgroups.service;

/** The User already owns a Group not in Trash with the same name ignoring case (R-002-03). */
public class GroupNameTakenException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public GroupNameTakenException() {
    super("You already have a group with this name");
  }
}
