package si.contactgroups.service;

/** The e-mail of a token already belongs to the User of another Firebase identifier (D-18d). */
public class EmailTakenException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public EmailTakenException() {
    super("e-mail belongs to another user");
  }
}
