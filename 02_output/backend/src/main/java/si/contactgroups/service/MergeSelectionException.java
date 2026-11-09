package si.contactgroups.service;

/**
 * A merge request that names fewer than two distinct Contacts or a Survivor that is not among them;
 * answered with 422 {@code merge.invalid_selection} (R-026-09).
 */
public class MergeSelectionException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public MergeSelectionException() {
    super("A merge needs at least two different Contacts of this Group, one of them the Survivor");
  }
}
