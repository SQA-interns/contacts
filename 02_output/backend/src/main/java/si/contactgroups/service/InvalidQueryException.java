package si.contactgroups.service;

/** A query parameter of a list request has a value the contract does not allow (400). */
public class InvalidQueryException extends RuntimeException {

  public InvalidQueryException(String message) {
    super(message);
  }
}
