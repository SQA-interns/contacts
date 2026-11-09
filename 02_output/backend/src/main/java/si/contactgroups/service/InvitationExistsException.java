package si.contactgroups.service;

/** The address is a Member of the Group or has a pending Invitation to it (R-014-04). */
public class InvitationExistsException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public InvitationExistsException() {
    super("This address is already a member or already invited");
  }
}
