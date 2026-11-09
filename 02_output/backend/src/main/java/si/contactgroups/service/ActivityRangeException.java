package si.contactgroups.service;

/** The start day of the activity filter is after the end day (R-017-06); answered with 422. */
public class ActivityRangeException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ActivityRangeException() {
    super("The start date is after the end date");
  }
}
