package si.contactgroups.security;

import java.io.IOException;

/** A request body turned out to be larger than the limit while it was being read (R-018-11). */
public class RequestTooLargeException extends IOException {

  private static final long serialVersionUID = 1L;

  public RequestTooLargeException() {
    super("The request body is too large");
  }
}
