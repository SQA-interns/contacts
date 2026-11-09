package si.contactgroups.service;

/** The User is no Member of the Group (R-015-03). */
public class MemberNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public MemberNotFoundException() {
    super("Member not found");
  }
}
