package si.contactgroups.service;

/** The Role of the Member does not allow the action (R-018-03, BR-02). */
public class RoleForbiddenException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public RoleForbiddenException() {
    super("Your role does not allow this");
  }
}
