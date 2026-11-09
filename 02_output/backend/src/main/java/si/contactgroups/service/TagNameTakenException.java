package si.contactgroups.service;

/** Another Tag of the Group has the name ignoring case (R-006-03, BR-05). */
public class TagNameTakenException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public TagNameTakenException() {
    super("Another tag already has this name");
  }
}
