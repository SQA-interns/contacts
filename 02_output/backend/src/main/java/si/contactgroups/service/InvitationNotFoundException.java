package si.contactgroups.service;

/** No pending Invitation with this id in the Group (R-014-07). */
public class InvitationNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public InvitationNotFoundException() {
    super("Invitation not found");
  }
}
