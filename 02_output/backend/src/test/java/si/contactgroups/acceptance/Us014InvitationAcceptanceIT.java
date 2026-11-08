package si.contactgroups.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.contactgroups.acceptance.support.AcceptanceTestBase;
import si.contactgroups.acceptance.support.Emulator;
import si.contactgroups.acceptance.support.Grp;
import si.contactgroups.acceptance.support.Json;
import si.contactgroups.acceptance.support.Resp;
import si.contactgroups.acceptance.support.TestUser;

/**
 * US-014 Invite a person to a Group. Invitations are accepted when the person opens the Group list.
 */
class Us014InvitationAcceptanceIT extends AcceptanceTestBase {

  private TestUser ana;
  private Grp acme;

  @BeforeEach
  void anaOwnsAcme() {
    ana = user("Ana Novak", "ana@acme.com");
    acme = group(ana, "Acme");
  }

  private Resp invite(String email, String role) {
    return ana.post(acme.invitations(), Json.obj("email", email, "role", role));
  }

  private List<JsonNode> pending() {
    return Json.list(ana.get(acme.invitations()).expect(200).json());
  }

  private void inviteMany(int count) {
    for (int i = 1; i <= count; i++) {
      invite("oseba" + i + "@acme.com", "VIEWER").expect(201);
    }
  }

  @Test
  @DisplayName(
      "AC-014-01 inviting boris@acme.com as Editor creates a pending Invitation expiring in 14 days")
  void ac_014_01_pending_invitation_14_days() {
    var created = invite("boris@acme.com", "EDITOR").expect(201).json();

    assertThat(created.path("status").asText()).isEqualTo("PENDING");
    assertThat(created.path("email").asText()).isEqualTo("boris@acme.com");
    assertThat(created.path("role").asText()).isEqualTo("EDITOR");
    Instant createdAt = Instant.parse(created.path("createdAt").asText());
    Instant expiresAt = Instant.parse(created.path("expiresAt").asText());
    assertThat(Duration.between(createdAt, expiresAt))
        .isBetween(Duration.ofDays(14).minusSeconds(1), Duration.ofDays(14).plusSeconds(1));
    List<JsonNode> list = pending();
    assertThat(list).hasSize(1);
    assertThat(list.get(0).path("email").asText()).isEqualTo("boris@acme.com");
    assertThat(list.get(0).path("role").asText()).isEqualTo("EDITOR");
    assertThat(list.get(0).path("expiresAt").asText())
        .isEqualTo(created.path("expiresAt").asText());
  }

  @Test
  @DisplayName("AC-014-02 Boris with a verified account signs in: Acme is listed with Role Editor")
  void ac_014_02_verified_invitee_joins() {
    invite("boris@acme.com", "EDITOR").expect(201);
    TestUser boris = account("Boris Kos", "boris@acme.com", true);

    assertThat(roleIn(boris, acme)).isEqualTo("EDITOR");
    assertThat(pending()).isEmpty();
  }

  @Test
  @DisplayName("AC-014-03 Boris with an unverified account gets no access to Acme")
  void ac_014_03_unverified_invitee_no_access() {
    invite("boris@acme.com", "EDITOR").expect(201);
    TestUser boris = account("Boris Kos", "boris@acme.com", false);

    boris.get("/groups").expectError(403, "auth.email_not_verified");

    assertThat(Json.field(ana.get(acme.members()).expect(200).json(), "email"))
        .doesNotContain("boris@acme.com");
    assertThat(pending()).hasSize(1);
  }

  @Test
  @DisplayName("AC-014-04 the address boris@ creates no Invitation")
  void ac_014_04_invalid_email_refused() {
    Resp answer = invite("boris@", "EDITOR").expect(422);

    assertThat(answer.errorFields()).contains("email");
    assertThat(pending()).isEmpty();
  }

  @Test
  @DisplayName("AC-014-05 a 21st pending Invitation is refused with the limit")
  void ac_014_05_more_than_20_pending_refused() {
    inviteMany(20);

    invite("dvajsetprvi@acme.com", "VIEWER").expectError(409, "invitation.limit");

    assertThat(pending()).hasSize(20);
  }

  @Test
  @DisplayName("AC-014-06 inviting Boris@Acme.com when boris@acme.com is a Member is refused")
  void ac_014_06_member_cannot_be_invited() {
    member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");

    invite("Boris@Acme.com", "VIEWER").expectError(409, "invitation.exists");

    assertThat(pending()).isEmpty();
  }

  @Test
  @DisplayName("AC-014-07 a cancelled Invitation gives Cene no access")
  void ac_014_07_cancelled_invitation_no_access() {
    String id = invite("cene@acme.com", "EDITOR").expect(201).id();

    var cancelled = ana.post(acme.cancelInvitation(id)).expect(200).json();
    TestUser cene = account("Cene Zupan", "cene@acme.com", true);

    assertThat(cancelled.path("status").asText()).isEqualTo("CANCELLED");
    assertThat(roleIn(cene, acme)).isNull();
    cene.get(acme.contacts()).expect(404);
  }

  @Test
  @DisplayName("AC-014-08 an Invitation created 14 days ago gives no access")
  void ac_014_08_expired_invitation_no_access() {
    clock.setNow(Instant.now().minus(Duration.ofDays(14)));
    invite("boris@acme.com", "EDITOR").expect(201);
    clock.reset();

    TestUser boris = account("Boris Kos", "boris@acme.com", true);

    assertThat(roleIn(boris, acme)).isNull();
  }

  @Test
  @DisplayName("AC-014-09 an Editor's invite request is refused")
  void ac_014_09_editor_cannot_invite() {
    TestUser boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
    int before = pending().size();

    boris
        .post(acme.invitations(), Json.obj("email", "cene@acme.com", "role", "VIEWER"))
        .expectError(403, "auth.role_forbidden");

    assertThat(pending()).hasSize(before);
  }

  @Test
  @DisplayName("AC-014-10 the Group activity list shows the invitation and the joining")
  void ac_014_10_invitation_and_joining_recorded() {
    invite("boris@acme.com", "EDITOR").expect(201);
    TestUser boris = account("Boris Kos", "boris@acme.com", true);
    boris.get("/groups").expect(200);

    List<String> actions = actions(activity(ana, acme, ""));

    assertThat(actions).contains("INVITATION_CREATED", "MEMBER_JOINED");
  }

  @Test
  @DisplayName("AC-014-11 creating an Invitation sends no e-mail")
  void ac_014_11_no_email_sent() {
    invite("boris@acme.com", "EDITOR").expect(201);

    assertThat(Emulator.sentEmails()).doesNotContain("boris@acme.com");
  }

  @Test
  @DisplayName("EX-014-01 with 19 pending Invitations a 20th is created")
  void ex_014_01_twentieth_created() {
    inviteMany(19);

    invite("dvajseti@acme.com", "VIEWER").expect(201);

    assertThat(pending()).hasSize(20);
  }

  @Test
  @DisplayName("EX-014-02 with 20 pending of which one expired, a new Invitation is created")
  void ex_014_02_expired_ones_do_not_count() {
    clock.setNow(Instant.now().minus(Duration.ofDays(14)));
    invite("stara@acme.com", "VIEWER").expect(201);
    clock.reset();
    inviteMany(19);

    invite("nova@acme.com", "VIEWER").expect(201);
  }

  @Test
  @DisplayName("EX-014-03 an Invitation 13 days 23 hours old is still valid")
  void ex_014_03_almost_14_days_valid() {
    clock.setNow(Instant.now().minus(Duration.ofDays(13).plusHours(23)));
    invite("boris@acme.com", "EDITOR").expect(201);
    clock.reset();

    TestUser boris = account("Boris Kos", "boris@acme.com", true);

    assertThat(roleIn(boris, acme)).isEqualTo("EDITOR");
  }

  @Test
  @DisplayName("EX-014-04 an Invitation exactly 14 days old is expired")
  void ex_014_04_exactly_14_days_expired() {
    var invitation = invite("boris@acme.com", "EDITOR").expect(201).json();
    clock.setNow(Instant.parse(invitation.path("createdAt").asText()).plus(Duration.ofDays(14)));

    TestUser boris = account("Boris Kos", "boris@acme.com", true);

    assertThat(roleIn(boris, acme)).isNull();
  }

  @Test
  @DisplayName("EX-014-05 an address with a pending Invitation cannot be invited again")
  void ex_014_05_second_invitation_refused() {
    invite("boris@acme.com", "EDITOR").expect(201);

    invite("boris@acme.com", "VIEWER").expectError(409, "invitation.exists");

    assertThat(pending()).hasSize(1);
  }

  @Test
  @DisplayName("EX-014-06 '  BORIS@ACME.COM ' is stored as boris@acme.com")
  void ex_014_06_email_trimmed_and_lowercase() {
    var created = invite("  BORIS@ACME.COM ", "EDITOR").expect(201).json();

    assertThat(created.path("email").asText()).isEqualTo("boris@acme.com");
    assertThat(pending().get(0).path("email").asText()).isEqualTo("boris@acme.com");
  }

  @Test
  @DisplayName("EX-014-07 a person who registers after the Invitation gets Acme after verifying")
  void ex_014_07_register_then_verify() {
    invite("boris@acme.com", "EDITOR").expect(201);
    TestUser boris = account("Boris Kos", "boris@acme.com", false);
    boris.get("/groups").expectError(403, "auth.email_not_verified");

    Emulator.setEmailVerified(boris.uid(), true);
    boris.refresh();

    assertThat(roleIn(boris, acme)).isEqualTo("EDITOR");
  }

  @Test
  @DisplayName("EX-014-08 inviting the Owner's own address is refused: already a Member")
  void ex_014_08_own_address_refused() {
    invite("ana@acme.com", "EDITOR").expectError(409, "invitation.exists");

    assertThat(pending()).isEmpty();
  }

  @Test
  @DisplayName("EX-014-09 a person signing in with another verified address gets no access")
  void ex_014_09_other_address_no_access() {
    invite("boris@acme.com", "EDITOR").expect(201);

    TestUser other = account("Boris Kos", "boris.kos@acme.com", true);

    assertThat(roleIn(other, acme)).isNull();
    assertThat(pending()).hasSize(1);
  }
}
