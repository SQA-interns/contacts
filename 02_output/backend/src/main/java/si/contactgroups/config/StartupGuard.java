package si.contactgroups.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Refuses to start in production with the Firebase emulator configured (threat model, S/token
 * check).
 */
@Component
public class StartupGuard implements InitializingBean {

  private final Environment environment;

  public StartupGuard(Environment environment) {
    this.environment = environment;
  }

  @Override
  public void afterPropertiesSet() {
    String emulator = environment.getProperty("FIREBASE_AUTH_EMULATOR_HOST", "");
    if (environment.acceptsProfiles(Profiles.of("prod")) && !emulator.isBlank()) {
      throw new IllegalStateException(
          "FIREBASE_AUTH_EMULATOR_HOST must be empty when the profile prod is active");
    }
  }
}
