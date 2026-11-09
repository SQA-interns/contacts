package si.contactgroups.service;

/** The change would leave the Group without an Owner (R-015-04). */
public class LastOwnerException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public LastOwnerException() {
    super("A group needs at least one owner");
  }
}
