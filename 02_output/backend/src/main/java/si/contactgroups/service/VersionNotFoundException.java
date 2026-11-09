package si.contactgroups.service;

/** The Contact has no Version with this number, or the number is not one (EX-022-03). */
public class VersionNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public VersionNotFoundException() {
    super("Version not found");
  }
}
