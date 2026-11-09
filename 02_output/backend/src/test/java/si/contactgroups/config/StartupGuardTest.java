package si.contactgroups.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

/**
 * Threat model: the Firebase emulator must never be accepted in production, and production must
 * hold the credentials that the Admin SDK needs to read an account (D-69, review F-14).
 */
class StartupGuardTest {

  @TempDir Path dir;

  @Test
  void prodProfileWithEmulatorHostIsRefused() throws Exception {
    MockEnvironment env =
        prodWithCredentials().withProperty("FIREBASE_AUTH_EMULATOR_HOST", "emulator:9099");
    assertThatThrownBy(() -> new StartupGuard(env).afterPropertiesSet())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("FIREBASE_AUTH_EMULATOR_HOST");
  }

  @Test
  void prodProfileWithoutEmulatorHostStarts() throws Exception {
    MockEnvironment env = prodWithCredentials();
    assertThatCode(() -> new StartupGuard(env).afterPropertiesSet()).doesNotThrowAnyException();
  }

  @Test
  void localProfileMayUseTheEmulator() {
    MockEnvironment env =
        new MockEnvironment().withProperty("FIREBASE_AUTH_EMULATOR_HOST", "emulator:9099");
    env.setActiveProfiles("local");
    assertThatCode(() -> new StartupGuard(env).afterPropertiesSet()).doesNotThrowAnyException();
  }

  @Test
  void emptyEmulatorHostIsTheSameAsUnset() throws Exception {
    MockEnvironment env = prodWithCredentials().withProperty("FIREBASE_AUTH_EMULATOR_HOST", "");
    assertThatCode(() -> new StartupGuard(env).afterPropertiesSet()).doesNotThrowAnyException();
  }

  @Test
  void prodProfileWithoutCredentialsIsRefused() {
    MockEnvironment env = new MockEnvironment();
    env.setActiveProfiles("prod");
    assertThatThrownBy(() -> new StartupGuard(env).afterPropertiesSet())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("GOOGLE_APPLICATION_CREDENTIALS");
  }

  @Test
  void prodProfileWithBlankCredentialsIsRefused() {
    MockEnvironment env = new MockEnvironment().withProperty("GOOGLE_APPLICATION_CREDENTIALS", "");
    env.setActiveProfiles("prod");
    assertThatThrownBy(() -> new StartupGuard(env).afterPropertiesSet())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("GOOGLE_APPLICATION_CREDENTIALS");
  }

  @Test
  void prodProfileWithAnUnreadableCredentialsFileIsRefused() {
    MockEnvironment env =
        new MockEnvironment()
            .withProperty("GOOGLE_APPLICATION_CREDENTIALS", dir.resolve("absent.json").toString());
    env.setActiveProfiles("prod");
    assertThatThrownBy(() -> new StartupGuard(env).afterPropertiesSet())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("GOOGLE_APPLICATION_CREDENTIALS");
  }

  @Test
  void localProfileNeedsNoCredentials() {
    MockEnvironment env = new MockEnvironment();
    env.setActiveProfiles("local");
    assertThatCode(() -> new StartupGuard(env).afterPropertiesSet()).doesNotThrowAnyException();
  }

  private MockEnvironment prodWithCredentials() throws Exception {
    MockEnvironment env =
        new MockEnvironment()
            .withProperty(
                "GOOGLE_APPLICATION_CREDENTIALS", ServiceAccountFiles.create(dir).toString());
    env.setActiveProfiles("prod");
    return env;
  }
}
