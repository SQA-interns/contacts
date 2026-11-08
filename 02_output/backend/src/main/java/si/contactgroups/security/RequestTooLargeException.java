package si.contactgroups.security;

import java.io.IOException;

/** A request body turned out to be larger than the limit while it was being read (R-018-11). */
public class RequestTooLargeException extends IOException {

  /** The code of the error format (ADR-10). */
  public static final String CODE = "request.too_large";

  private static final long serialVersionUID = 1L;

  public RequestTooLargeException() {
    super("The request body is too large");
  }
}
