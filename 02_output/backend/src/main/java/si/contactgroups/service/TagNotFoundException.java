package si.contactgroups.service;

/** The Tag does not exist in this Group or the id is not a Tag id (BR-01). */
public class TagNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public TagNotFoundException() {
    super("Tag not found");
  }
}
