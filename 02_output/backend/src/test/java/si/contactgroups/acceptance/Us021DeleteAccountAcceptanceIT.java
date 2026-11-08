package si.contactgroups.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
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

/** US-021 Delete my account ({@code DELETE /me}). */
class Us021DeleteAccountAcceptanceIT extends AcceptanceTestBase {

  private TestUser ana;
  private TestUser boris;
  private Grp acme;

  @BeforeEach
  void borisEditsAcme() {
    ana = user("Ana Novak", "ana@acme.com");
    acme = group(ana, "Acme");
    boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
  }

  private List<String> memberEmails() {
    return Json.field(ana.get(acme.members()).expect(200).json(), "email");
  }

  @Test
  @DisplayName("AC-021-01 Boris deletes his account: it is gone and Acme no longer lists him")
  void ac_021_01_account_deleted() {
    boris.delete("/me").expect(204);

    assertThat(Emulator.accountExists("boris@acme.com")).isFalse();
    assertThat(memberEmails()).doesNotContain("boris@acme.com");
  }

  @Test
  @DisplayName("AC-021-02 the Sole Owner of Acme cannot delete her account; Acme is listed")
  void ac_021_02_sole_owner_refused() {
    Resp answer = ana.delete("/me").expectError(409, "account.sole_owner");

    List<JsonNode> groups = Json.list(answer.json().path("groups"));
    assertThat(groups).hasSize(1);
    assertThat(groups.get(0).path("id").asText()).isEqualTo(acme.id());
    assertThat(groups.get(0).path("name").asText()).isEqualTo("Acme");
    assertThat(groups.get(0).path("inTrash").asBoolean()).isFalse();
    assertThat(Emulator.accountExists("ana@acme.com")).isTrue();
    assertThat(roleIn(ana, acme)).isEqualTo("OWNER");
  }

  @Test
  @DisplayName("AC-021-03 with a second Owner Ana can delete her account")
  void ac_021_03_second_owner_allows_deletion() {
    TestUser cene = member(acme, ana, "Cene Zupan", "cene@acme.com", "OWNER");

    ana.delete("/me").expect(204);

    assertThat(Emulator.accountExists("ana@acme.com")).isFalse();
    assertThat(Json.field(cene.get(acme.members()).expect(200).json(), "email"))
        .doesNotContain("ana@acme.com");
  }

  @Test
  @DisplayName("AC-021-04 Boris's History entries keep his name and lose his e-mail address")
  void ac_021_04_history_keeps_name_without_email() {
    String id =
        createContact(ana, acme, "firstName", "Ana", "lastName", "Novak").path("id").asText();
    edit(boris, acme, id, "city", "Celje");

    boris.delete("/me").expect(204);

    JsonNode entry = history(ana, acme, id).get(0);
    assertThat(entry.path("userName").asText()).isEqualTo("Boris Kos");
    assertThat(entry.path("userEmail").isMissingNode() || entry.path("userEmail").isNull())
        .isTrue();
    assertThat(entry.toString()).doesNotContain("boris@acme.com");
  }

  @Test
  @DisplayName("AC-021-05 the Contacts Boris created stay unchanged")
  void ac_021_05_contacts_kept() {
    JsonNode created = createContact(boris, acme, "firstName", "Borisov", "lastName", "Kontakt");

    boris.delete("/me").expect(204);

    JsonNode after = getContact(ana, acme, created.path("id").asText());
    assertThat(after.path("lastName").asText()).isEqualTo("Kontakt");
    assertThat(after.path("version").asInt()).isEqualTo(created.path("version").asInt());
  }

  @Test
  @DisplayName("AC-021-06 signing in with the deleted account fails")
  void ac_021_06_sign_in_fails() {
    String oldToken = boris.token();

    boris.delete("/me").expect(204);

    assertThat(Emulator.accountExists("boris@acme.com")).isFalse();
    http.send("GET", "/groups", oldToken, null).expect(401);
  }

  @Test
  @DisplayName("AC-021-07 registering boris@acme.com again gives a new account without Memberships")
  void ac_021_07_new_account_without_memberships() {
    String oldId = boris.userId();
    boris.delete("/me").expect(204);

    TestUser again = user("Boris Kos", "boris@acme.com");

    assertThat(again.userId()).isNotEqualTo(oldId);
    assertThat(again.get("/groups").expect(200).json()).isEmpty();
    again.get(acme.contacts()).expect(404);
  }

  @Test
  @DisplayName("EX-021-01 a Sole Owner of 2 Groups gets both listed")
  void ex_021_01_two_groups_listed() {
    Grp beta = group(ana, "Beta");

    Resp answer = ana.delete("/me").expectError(409, "account.sole_owner");

    assertThat(Json.field(answer.json().path("groups"), "id"))
        .containsExactlyInAnyOrder(acme.id(), beta.id());
  }

  @Test
  @DisplayName("EX-021-02 a User with no Group can delete the account")
  void ex_021_02_no_group() {
    TestUser cene = user("Cene Zupan", "cene@acme.com");

    cene.delete("/me").expect(204);

    assertThat(Emulator.accountExists("cene@acme.com")).isFalse();
  }

  @Test
  @DisplayName("EX-021-04 a pending Invitation to the deleted address stays pending")
  void ex_021_04_invitation_stays_pending() {
    TestUser cene = user("Cene Zupan", "cene@acme.com");
    ana.post(acme.invitations(), Json.obj("email", "cene@acme.com", "role", "VIEWER")).expect(201);

    cene.delete("/me").expect(204);

    List<JsonNode> pending = Json.list(ana.get(acme.invitations()).expect(200).json());
    assertThat(Json.field(Json.MAPPER.valueToTree(pending), "email"))
        .containsExactly("cene@acme.com");
  }
}
