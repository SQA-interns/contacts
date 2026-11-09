package si.contactgroups.service;

/** The Group already has 20 pending Invitations (R-014-03). */
public class InvitationLimitException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public InvitationLimitException() {
    super("A group can have at most 20 pending invitations");
  }
}
