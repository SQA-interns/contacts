package si.contactgroups.security;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import org.springframework.stereotype.Component;

/**
 * Deletes and looks up Firebase accounts with the Admin SDK (ADR-04). Like {@link
 * FirebaseTokenVerifier} it keeps {@code FirebaseAuth} out of the Spring context.
 */
@Component
public class FirebaseAccountDirectory implements AccountDirectory {

  private final FirebaseAuth firebaseAuth;

  public FirebaseAccountDirectory(FirebaseApp firebaseApp) {
    this.firebaseAuth = FirebaseAuth.getInstance(firebaseApp);
  }

  @Override
  public void delete(String firebaseUid) {
    try {
      firebaseAuth.deleteUser(firebaseUid);
    } catch (FirebaseAuthException e) {
      if (e.getAuthErrorCode() != AuthErrorCode.USER_NOT_FOUND) {
        throw new IllegalStateException("Firebase account not deleted", e);
      }
    }
  }

  @Override
  public boolean exists(String firebaseUid) {
    try {
      firebaseAuth.getUser(firebaseUid);
      return true;
    } catch (FirebaseAuthException e) {
      if (e.getAuthErrorCode() == AuthErrorCode.USER_NOT_FOUND) {
        return false;
      }
      throw new IllegalStateException("Firebase account not read", e);
    }
  }
}
