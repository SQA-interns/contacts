package si.contactgroups.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/** Threat model: the Firebase emulator must never be accepted in production. */
class StartupGuardTest {

  @Test
  void prodProfileWithEmulatorHostIsRefused() {
    MockEnvironment env =
        new MockEnvironment().withProperty("FIREBASE_AUTH_EMULATOR_HOST", "emulator:9099");
    env.setActiveProfiles("prod");
    assertThatThrownBy(() -> new StartupGuard(env).afterPropertiesSet())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("FIREBASE_AUTH_EMULATOR_HOST");
  }

  @Test
  void prodProfileWithoutEmulatorHostStarts() {
    MockEnvironment env = new MockEnvironment();
    env.setActiveProfiles("prod");
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
  void emptyEmulatorHostIsTheSameAsUnset() {
    MockEnvironment env = new MockEnvironment().withProperty("FIREBASE_AUTH_EMULATOR_HOST", "");
    env.setActiveProfiles("prod");
    assertThatCode(() -> new StartupGuard(env).afterPropertiesSet()).doesNotThrowAnyException();
  }
}
