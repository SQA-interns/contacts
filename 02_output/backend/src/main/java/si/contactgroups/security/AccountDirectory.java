package si.contactgroups.security;

/** The accounts of Firebase Authentication that the product deletes and checks (US-021). */
public interface AccountDirectory {

  /**
   * Deletes the Firebase account.
   *
   * @throws IllegalStateException when Firebase cannot delete it
   */
  void delete(String firebaseUid);

  /** Whether Firebase still holds an account with this identifier (R-021-05). */
  boolean exists(String firebaseUid);
}
