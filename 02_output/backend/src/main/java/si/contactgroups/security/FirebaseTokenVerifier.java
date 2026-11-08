package si.contactgroups.security;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.stereotype.Component;

/**
 * Verifies ID tokens with the Firebase Admin SDK (ADR-04). {@code FirebaseAuth} is deliberately not
 * a Spring bean: its class references Google libraries that are excluded (D-12), and Spring would
 * introspect it.
 */
@Component
public class FirebaseTokenVerifier implements TokenVerifier {

  private final FirebaseAuth firebaseAuth;

  public FirebaseTokenVerifier(FirebaseApp firebaseApp) {
    this.firebaseAuth = FirebaseAuth.getInstance(firebaseApp);
  }

  @Override
  public VerifiedToken verify(String idToken) {
    try {
      FirebaseToken token = firebaseAuth.verifyIdToken(idToken, false);
      String email = token.getEmail();
      if (email == null || email.isBlank()) {
        throw new InvalidTokenException("token has no e-mail", null);
      }
      return new VerifiedToken(token.getUid(), email, token.isEmailVerified(), token.getName());
    } catch (FirebaseAuthException | IllegalArgumentException e) {
      throw new InvalidTokenException("token rejected", e);
    }
  }
}
