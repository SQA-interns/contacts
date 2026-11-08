package si.contactgroups.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.contactgroups.acceptance.support.AcceptanceTestBase;
import si.contactgroups.acceptance.support.Emulator;
import si.contactgroups.acceptance.support.Json;
import si.contactgroups.acceptance.support.TestUser;

/**
 * US-001 Register and sign in: the backend side (R-001-03, R-001-04, R-001-11, R-001-12). The
 * screens of US-001 are tested in {@code frontend/acceptance/us001-account.spec.ts}.
 */
class Us001AccountAcceptanceIT extends AcceptanceTestBase {

  @Test
  @DisplayName("AC-001-02 a verified sign-in opens the Group list and creates the User")
  void ac_001_02_verified_sign_in_creates_user() {
    TestUser ana = account("Ana Novak", "ana@acme.com", true);

    ana.get("/groups").expect(200);
    var me = ana.get("/me").expect(200).json();

    assertThat(me.path("email").asText()).isEqualTo("ana@acme.com");
    assertThat(me.path("displayName").asText()).isEqualTo("Ana Novak");
    assertThat(me.path("emailVerified").asBoolean()).isTrue();
  }

  @Test
  @DisplayName("AC-001-03 Sign in with Google uses the name of the Google profile")
  void ac_001_03_google_sign_in_uses_profile_name() {
    String token = Emulator.signInWithGoogle("janez@acme.com", "Janez Kos");

    var me = http.send("GET", "/me", token, null).expect(200).json();
    http.send("GET", "/groups", token, null).expect(200);

    assertThat(me.path("displayName").asText()).isEqualTo("Janez Kos");
    assertThat(me.path("email").asText()).isEqualTo("janez@acme.com");
  }

  @Test
  @DisplayName("AC-001-04 a User with an unverified e-mail gets no Group")
  void ac_001_04_unverified_user_gets_no_group() {
    TestUser ana = account("Ana Novak", "ana@acme.com", false);

    ana.get("/groups").expectError(403, "auth.email_not_verified");
    ana.post("/groups", Json.obj("name", "Acme")).expectError(403, "auth.email_not_verified");
  }

  @Test
  @DisplayName("AC-001-11 an expired token is answered 401, so the client returns to sign-in")
  void ac_001_11_expired_token_is_401() {
    TestUser ana = user("Ana Novak", "ana@acme.com");
    Instant now = Instant.now();
    Emulator.setValidSince(ana.uid(), now.minusSeconds(5 * 3600));
    String expired =
        Emulator.forgedToken(
            Emulator.PROJECT_ID, ana.uid(), ana.email(), now.minusSeconds(3 * 3600), now.minusSeconds(2 * 3600));

    var answer = http.send("GET", "/groups", expired, null).expect(401);

    assertThat(answer.code()).isEqualTo("auth.token_invalid");
  }

  @Test
  @DisplayName("EX-001-05 the e-mail Ana@Acme.COM is stored as ana@acme.com")
  void ex_001_05_email_stored_lowercase() {
    TestUser ana = account("Ana Novak", "Ana@Acme.COM", true);

    var me = ana.get("/me").expect(200).json();

    assertThat(me.path("email").asText()).isEqualTo("ana@acme.com");
  }

  @Test
  @DisplayName("EX-001-07 a Google profile without a name gets the e-mail address as display name")
  void ex_001_07_google_without_name_uses_email() {
    String token = Emulator.signInWithGoogle("janez@acme.com", null);

    var me = http.send("GET", "/me", token, null).expect(200).json();

    assertThat(me.path("displayName").asText()).isEqualTo("janez@acme.com");
  }

  @Test
  @DisplayName("EX-001-09 after verifying the e-mail and signing in again the Group list opens")
  void ex_001_09_verified_after_reload_opens_group_list() {
    TestUser ana = account("Ana Novak", "ana@acme.com", false);
    ana.get("/groups").expectError(403, "auth.email_not_verified");

    Emulator.setEmailVerified(ana.uid(), true);
    ana.refresh();

    ana.get("/groups").expect(200);
  }
}
