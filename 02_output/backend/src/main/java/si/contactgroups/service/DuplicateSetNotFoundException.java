package si.contactgroups.service;

/**
 * The Contacts are not a Duplicate set of this Group: they do not exist, belong to another Group or
 * are in Trash, or they no longer form a set (EX-025-03).
 */
public class DuplicateSetNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public DuplicateSetNotFoundException() {
    super("Duplicate set not found");
  }
}
