package si.contactgroups.service;

/** The name typed to confirm the deletion of a Group differs from the Group name (R-020-02). */
public class GroupNameMismatchException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public GroupNameMismatchException() {
    super("The typed name does not match the group name");
  }
}
