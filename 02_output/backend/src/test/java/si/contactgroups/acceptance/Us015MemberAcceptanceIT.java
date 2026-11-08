package si.contactgroups.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.contactgroups.acceptance.support.AcceptanceTestBase;
import si.contactgroups.acceptance.support.Db;
import si.contactgroups.acceptance.support.Grp;
import si.contactgroups.acceptance.support.Json;
import si.contactgroups.acceptance.support.Resp;
import si.contactgroups.acceptance.support.TestUser;

/** US-015 Manage Members and Roles. */
class Us015MemberAcceptanceIT extends AcceptanceTestBase {

  private TestUser ana;
  private TestUser boris;
  private TestUser viola;
  private Grp acme;

  @BeforeEach
  void acmeWithThreeMembers() {
    ana = user("Ana Novak", "ana@acme.com");
    acme = group(ana, "Acme");
    boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
    viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");
  }

  private List<JsonNode> members(TestUser user) {
    return Json.list(user.get(acme.members()).expect(200).json());
  }

  private String userIdOf(String email) {
    return members(ana).stream()
        .filter(m -> m.path("email").asText().equals(email))
        .map(m -> m.path("userId").asText())
        .findFirst()
        .orElseThrow();
  }

  private Resp changeRole(TestUser by, String email, String role) {
    return by.patch(acme.member(userIdOf(email)), Json.obj("role", role));
  }

  @Test
  @DisplayName("AC-015-01 Viola sees the three Members with names, e-mail addresses and Roles")
  void ac_015_01_viewer_sees_member_list() {
    List<JsonNode> list = members(viola);

    assertThat(list).hasSize(3);
    assertThat(Json.field(Json.MAPPER.valueToTree(list), "displayName"))
        .containsExactlyInAnyOrder("Ana Novak", "Boris Kos", "Viola Vidmar");
    assertThat(Json.field(Json.MAPPER.valueToTree(list), "email"))
        .containsExactlyInAnyOrder("ana@acme.com", "boris@acme.com", "viola@acme.com");
    assertThat(Json.field(Json.MAPPER.valueToTree(list), "role"))
        .containsExactlyInAnyOrder("OWNER", "EDITOR", "VIEWER");
  }

  @Test
  @DisplayName("AC-015-02 after Ana changes Boris to Viewer his next create request is refused")
  void ac_015_02_role_change_applies_at_once() {
    boris.post(acme.contacts(), contactBody("firstName", "Prvi")).expect(201);

    changeRole(ana, "boris@acme.com", "VIEWER").expect(200);

    boris
        .post(acme.contacts(), contactBody("firstName", "Drugi"))
        .expectError(403, "auth.role_forbidden");
    assertThat(contactPage(ana, acme, "").path("total").asInt()).isEqualTo(1);
    assertThat(roleIn(boris, acme)).isEqualTo("VIEWER");
  }

  @Test
  @DisplayName("AC-015-03 a removed Member gets not found for Acme and no longer lists it")
  void ac_015_03_removed_member_loses_access() {
    ana.delete(acme.member(userIdOf("boris@acme.com"))).expect(204);

    boris.get(acme.contacts()).expect(404);
    assertThat(roleIn(boris, acme)).isNull();
    assertThat(Json.field(Json.MAPPER.valueToTree(members(ana)), "email"))
        .doesNotContain("boris@acme.com");
  }

  @Test
  @DisplayName("AC-015-04 the only Owner can neither demote nor remove herself")
  void ac_015_04_last_owner_kept() {
    changeRole(ana, "ana@acme.com", "EDITOR").expectError(409, "group.last_owner");
    ana.delete(acme.member(userIdOf("ana@acme.com"))).expectError(409, "group.last_owner");

    assertThat(roleIn(ana, acme)).isEqualTo("OWNER");
  }

  @Test
  @DisplayName("AC-015-05 with Cene as second Owner Ana may become Editor")
  void ac_015_05_second_owner_allows_demotion() {
    TestUser cene = member(acme, ana, "Cene Zupan", "cene@acme.com", "OWNER");

    changeRole(ana, "ana@acme.com", "EDITOR").expect(200);

    assertThat(roleIn(ana, acme)).isEqualTo("EDITOR");
    assertThat(roleIn(cene, acme)).isEqualTo("OWNER");
  }

  @Test
  @DisplayName("AC-015-06 an Editor's Role change is refused")
  void ac_015_06_editor_cannot_change_roles() {
    changeRole(boris, "viola@acme.com", "EDITOR").expectError(403, "auth.role_forbidden");
    boris.delete(acme.member(userIdOf("viola@acme.com"))).expectError(403, "auth.role_forbidden");

    assertThat(roleIn(viola, acme)).isEqualTo("VIEWER");
  }

  @Test
  @DisplayName(
      "AC-015-07 a Role change and a removal give two History entries with Members and Roles")
  void ac_015_07_changes_recorded() {
    changeRole(ana, "viola@acme.com", "EDITOR").expect(200);
    ana.delete(acme.member(userIdOf("boris@acme.com"))).expect(204);

    JsonNode changed = activity(ana, acme, "action=ROLE_CHANGED").get(0);
    JsonNode removed = activity(ana, acme, "action=MEMBER_REMOVED").get(0);

    assertThat(changed.path("details").toString()).containsAnyOf("Viola Vidmar", "viola@acme.com");
    assertThat(changed.path("details").toString()).contains("VIEWER").contains("EDITOR");
    assertThat(removed.path("details").toString()).containsAnyOf("Boris Kos", "boris@acme.com");
    assertThat(changed.path("userName").asText()).isEqualTo("Ana Novak");
  }

  @Test
  @DisplayName(
      "AC-015-08 the Contact Boris created stays after his removal and its History shows his name")
  void ac_015_08_removed_members_work_kept() {
    String id =
        createContact(boris, acme, "firstName", "Borisov", "lastName", "Kontakt")
            .path("id")
            .asText();

    ana.delete(acme.member(userIdOf("boris@acme.com"))).expect(204);

    assertThat(getContact(ana, acme, id).path("lastName").asText()).isEqualTo("Kontakt");
    List<JsonNode> entries = history(ana, acme, id);
    assertThat(entries.get(entries.size() - 1).path("userName").asText()).isEqualTo("Boris Kos");
  }

  @Test
  @DisplayName("AC-015-09 a non-Member gets not found and no data for the Member list")
  void ac_015_09_non_member_not_found() {
    TestUser cene = user("Cene Zupan", "cene@acme.com");

    Resp answer = cene.get(acme.members()).expect(404);

    assertThat(answer.body()).doesNotContain("ana@acme.com").doesNotContain("Boris");
  }

  @Test
  @DisplayName("EX-015-01 two Owners demoting each other at once: one succeeds, one Owner remains")
  void ex_015_01_concurrent_demotions_keep_an_owner() throws Exception {
    TestUser cene = member(acme, ana, "Cene Zupan", "cene@acme.com", "OWNER");
    String anaId = userIdOf("ana@acme.com");
    String ceneId = userIdOf("cene@acme.com");

    List<Resp> answers =
        atOnce(
            () -> ana.patch(acme.member(ceneId), Json.obj("role", "EDITOR")),
            () -> cene.patch(acme.member(anaId), Json.obj("role", "EDITOR")));

    assertThat(answers.stream().filter(a -> a.status() == 200)).hasSize(1);
    long owners =
        members(boris).stream().filter(m -> m.path("role").asText().equals("OWNER")).count();
    assertThat(owners).isEqualTo(1);
  }

  @Test
  @DisplayName(
      "EX-015-02 changing a Member to the Role the Member has changes nothing and records nothing")
  void ex_015_02_same_role_no_entry() {
    changeRole(ana, "boris@acme.com", "EDITOR");

    assertThat(roleIn(boris, acme)).isEqualTo("EDITOR");
    assertThat(activity(ana, acme, "action=ROLE_CHANGED")).isEmpty();
  }

  @Test
  @DisplayName("EX-015-03 a Member removed while editing gets not found on save")
  void ex_015_03_removed_while_editing() {
    String id = createContact(ana, acme, "firstName", "Ana").path("id").asText();
    JsonNode opened = getContact(boris, acme, id);

    ana.delete(acme.member(userIdOf("boris@acme.com"))).expect(204);

    save(boris, acme, opened, "company", "Beta").expect(404);
    assertThat(getContact(ana, acme, id).path("company").asText("")).isEmpty();
  }

  @Test
  @DisplayName("EX-015-04 removing someone who is not a Member is not found")
  void ex_015_04_remove_non_member() {
    TestUser cene = user("Cene Zupan", "cene@acme.com");

    ana.delete(acme.member(cene.userId())).expect(404);
    ana.delete(acme.member(Db.randomId())).expect(404);

    assertThat(members(ana)).hasSize(3);
  }

  @Test
  @DisplayName("EX-015-05 a removed Member may be invited again")
  void ex_015_05_removed_member_invited_again() {
    ana.delete(acme.member(userIdOf("boris@acme.com"))).expect(204);

    ana.post(acme.invitations(), Json.obj("email", "boris@acme.com", "role", "VIEWER")).expect(201);

    assertThat(roleIn(boris, acme)).isEqualTo("VIEWER");
  }
}
