package si.contactgroups.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Refuses to start in production with the Firebase emulator configured (threat model, S/token
 * check) or without the service account that the Admin SDK needs to read and delete accounts (D-69,
 * review F-14): a missing key then shows at deployment, not at the first sign-in.
 */
@Component
public class StartupGuard implements InitializingBean {

  private final Environment environment;

  public StartupGuard(Environment environment) {
    this.environment = environment;
  }

  @Override
  public void afterPropertiesSet() {
    if (!environment.acceptsProfiles(Profiles.of("prod"))) {
      return;
    }
    if (!environment.getProperty("FIREBASE_AUTH_EMULATOR_HOST", "").isBlank()) {
      throw new IllegalStateException(
          "FIREBASE_AUTH_EMULATOR_HOST must be empty when the profile prod is active");
    }
    if (environment.getProperty(FirebaseCredentials.VARIABLE, "").isBlank()) {
      throw new IllegalStateException(
          FirebaseCredentials.VARIABLE
              + " must name the Firebase service-account file when the profile prod is active");
    }
    FirebaseCredentials.load(environment);
  }
}
