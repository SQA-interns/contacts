package si.contactgroups.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
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

/** US-020 Delete a Group (Group Trash, restore, clean-up after 30 days). */
class Us020DeleteGroupAcceptanceIT extends AcceptanceTestBase {

  private TestUser ana;
  private TestUser boris;
  private Grp acme;
  private String contact;

  @BeforeEach
  void acmeWithEditorAndContact() {
    ana = user("Ana Novak", "ana@acme.com");
    acme = group(ana, "Acme");
    boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
    contact = createContact(ana, acme, "firstName", "Ana", "lastName", "Novak").path("id").asText();
    ana.post(acme.invitations(), Json.obj("email", "cene@acme.com", "role", "VIEWER")).expect(201);
  }

  private Resp trashGroup(TestUser by, String typed) {
    return by.post(acme.toGroupTrash(), Json.obj("name", typed));
  }

  private List<JsonNode> groupTrash(TestUser user) {
    return Json.list(user.get("/group-trash").expect(200).json());
  }

  private List<String> groupIds(TestUser user) {
    return Json.field(user.get("/groups").expect(200).json(), "id");
  }

  @Test
  @DisplayName(
      "AC-020-01 typing Acme moves it from the Group list to the Group Trash with 30 days left")
  void ac_020_01_group_to_trash() {
    trashGroup(ana, "Acme").expect(204);

    assertThat(groupIds(ana)).doesNotContain(acme.id());
    List<JsonNode> trash = groupTrash(ana);
    assertThat(trash).hasSize(1);
    assertThat(trash.get(0).path("name").asText()).isEqualTo("Acme");
    assertThat(trash.get(0).path("daysLeft").asInt()).isEqualTo(30);
  }

  @Test
  @DisplayName("AC-020-02 typing acme does not delete the Group")
  void ac_020_02_wrong_case_refused() {
    trashGroup(ana, "acme").expectError(422, "group.name_mismatch");

    assertThat(groupIds(ana)).contains(acme.id());
    assertThat(groupTrash(ana)).isEmpty();
  }

  @Test
  @DisplayName(
      "AC-020-03 Boris (Editor) gets not found and no data for the Contacts of Acme in Trash")
  void ac_020_03_group_in_trash_not_found() {
    trashGroup(ana, "Acme").expect(204);

    Resp answer = boris.get(acme.contacts()).expect(404);

    assertThat(answer.body()).doesNotContain("Novak");
    assertThat(groupIds(boris)).doesNotContain(acme.id());
  }

  @Test
  @DisplayName(
      "AC-020-04 after 30 days the clean-up removes Group, Contacts, History, Members, Invitations")
  void ac_020_04_clean_up_after_30_days() {
    trashGroup(ana, "Acme").expect(204);

    clock.advance(Duration.ofDays(30).plusMinutes(1));

    awaitCleanUp(() -> Db.countWhere("contact_group", "id", acme.id()) == 0);
    assertThat(Db.countWhere("contact", "group_id", acme.id())).isZero();
    assertThat(Db.countWhere("history_entry", "group_id", acme.id())).isZero();
    assertThat(Db.countWhere("member", "group_id", acme.id())).isZero();
    assertThat(Db.countWhere("invitation", "group_id", acme.id())).isZero();
    assertThat(Db.countWhere("tag", "group_id", acme.id())).isZero();
    assertThat(Db.countWhere("contact_version", "contact_id", contact)).isZero();
  }

  @Test
  @DisplayName("AC-020-05 Boris's delete request is refused; Acme is unchanged")
  void ac_020_05_editor_cannot_delete() {
    trashGroup(boris, "Acme").expectError(403, "auth.role_forbidden");

    assertThat(groupIds(ana)).contains(acme.id());
    assertThat(contactPage(ana, acme, "").path("total").asInt()).isEqualTo(1);
  }

  @Test
  @DisplayName(
      "AC-020-06 restored after 3 days, Acme is back for Ana and Boris with Contacts and History")
  void ac_020_06_restore_within_30_days() {
    trashGroup(ana, "Acme").expect(204);
    clock.advance(Duration.ofDays(3));

    ana.post(acme.restoreGroup()).expect(200);

    assertThat(groupIds(ana)).contains(acme.id());
    assertThat(groupIds(boris)).contains(acme.id());
    assertThat(contactPage(boris, acme, "").path("total").asInt()).isEqualTo(1);
    assertThat(actions(history(ana, acme, contact))).contains("CONTACT_CREATED");
    assertThat(groupTrash(ana)).isEmpty();
  }

  @Test
  @DisplayName("AC-020-07 restoring Acme while Ana owns ACME is refused; Acme stays in Trash")
  void ac_020_07_restore_with_name_taken_refused() {
    trashGroup(ana, "Acme").expect(204);
    group(ana, "ACME");

    ana.post(acme.restoreGroup()).expectError(409, "group.name_taken");

    assertThat(Json.field(Json.MAPPER.valueToTree(groupTrash(ana)), "id"))
        .containsExactly(acme.id());
  }

  @Test
  @DisplayName("EX-020-01 the name typed with leading and trailing spaces is accepted")
  void ex_020_01_typed_name_trimmed() {
    trashGroup(ana, "  Acme  ").expect(204);

    assertThat(groupIds(ana)).doesNotContain(acme.id());
  }

  @Test
  @DisplayName("EX-020-02 a Group name of 100 characters can be typed to confirm")
  void ex_020_02_long_name() {
    String name = chars(100, 'g');
    Grp longOne = group(ana, name);

    ana.post(longOne.toGroupTrash(), Json.obj("name", name)).expect(204);

    assertThat(groupIds(ana)).doesNotContain(longOne.id());
  }

  @Test
  @DisplayName("EX-020-03 another Owner of the Group sees it in the Group Trash too")
  void ex_020_03_other_owner_sees_it() {
    TestUser cene = member(acme, ana, "Cene Zupan", "cene.owner@acme.com", "OWNER");

    trashGroup(ana, "Acme").expect(204);

    assertThat(Json.field(Json.MAPPER.valueToTree(groupTrash(cene)), "id"))
        .containsExactly(acme.id());
    assertThat(groupTrash(boris)).isEmpty();
  }

  @Test
  @DisplayName("EX-020-04 a Member saving a Contact after the Group was deleted gets not found")
  void ex_020_04_save_after_group_deleted() {
    JsonNode opened = getContact(boris, acme, contact);

    trashGroup(ana, "Acme").expect(204);

    save(boris, acme, opened, "company", "Beta").expect(404);
  }

  @Test
  @DisplayName(
      "EX-020-05 after 29 days 23 hours the Group is still in the Group Trash and can be restored")
  void ex_020_05_just_before_30_days() {
    trashGroup(ana, "Acme").expect(204);
    clock.advance(Duration.ofDays(29).plusHours(23));
    letCleanUpRun();

    assertThat(groupTrash(ana)).hasSize(1);
    ana.post(acme.restoreGroup()).expect(200);
    assertThat(groupIds(ana)).contains(acme.id());
  }

  @Test
  @DisplayName("EX-020-06 a restore at 30 days 0 minutes is not found")
  void ex_020_06_restore_at_30_days_not_found() {
    trashGroup(ana, "Acme").expect(204);
    clock.advance(Duration.ofDays(30));

    ana.post(acme.restoreGroup()).expect(404);

    assertThat(groupIds(ana)).doesNotContain(acme.id());
  }

  @Test
  @DisplayName("EX-020-07 an Editor or a Viewer asking to restore the Group is refused")
  void ex_020_07_editor_and_viewer_cannot_restore() {
    TestUser viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");
    trashGroup(ana, "Acme").expect(204);

    assertThat(boris.post(acme.restoreGroup()).status()).isBetween(400, 499);
    assertThat(viola.post(acme.restoreGroup()).status()).isBetween(400, 499);

    assertThat(groupTrash(ana)).hasSize(1);
    assertThat(groupIds(boris)).doesNotContain(acme.id());
  }
}
