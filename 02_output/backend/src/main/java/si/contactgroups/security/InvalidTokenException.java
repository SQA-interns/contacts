package si.contactgroups.security;

/** The ID token is missing, malformed, expired or not signed by Firebase. */
public class InvalidTokenException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public InvalidTokenException(String message, Throwable cause) {
    super(message, cause);
  }
}
