package si.contactgroups.service;

/**
 * An edit that does not carry the version number it is based on (contract {@code ContactInput}).
 */
public class VersionMissingException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public VersionMissingException() {
    super("The version number is required to edit a Contact");
  }
}
