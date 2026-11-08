package si.contactgroups.security;

/**
 * Checks a Firebase ID token (AR-06). A fake is used in unit tests; the emulator in integration
 * tests.
 */
public interface TokenVerifier {

  /**
   * @return the claims of a valid token
   * @throws InvalidTokenException when the token is not valid
   */
  VerifiedToken verify(String idToken);
}
