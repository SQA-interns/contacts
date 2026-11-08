package si.contactgroups.security;

/**
 * The caller of a request, built from the verified token only. It carries no Role: the Role is read
 * from the database on every request (SR-03).
 */
public record AuthenticatedUser(
    String firebaseUid, String email, boolean emailVerified, String name) {

  public static AuthenticatedUser from(VerifiedToken token) {
    return new AuthenticatedUser(
        token.uid(),
        token.email().toLowerCase(java.util.Locale.ROOT),
        token.emailVerified(),
        token.name());
  }
}
