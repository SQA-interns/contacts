package si.contactgroups.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.contactgroups.acceptance.support.AcceptanceTestBase;
import si.contactgroups.acceptance.support.Exports;
import si.contactgroups.acceptance.support.Grp;
import si.contactgroups.acceptance.support.Json;
import si.contactgroups.acceptance.support.Resp;
import si.contactgroups.acceptance.support.TestUser;
import si.contactgroups.acceptance.support.Xlsx;

/** US-012 Delete a Contact and restore it from Trash. */
class Us012TrashAcceptanceIT extends AcceptanceTestBase {

  private TestUser ana;
  private TestUser boris;
  private Grp acme;
  private String anaNovak;

  @BeforeEach
  void borisEditsAcme() {
    ana = user("Ana Novak", "ana@acme.com");
    acme = group(ana, "Acme");
    boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
    anaNovak =
        createContact(
                ana,
                acme,
                "firstName",
                "Ana",
                "lastName",
                "Novak",
                "email",
                "ana.novak@acme.com",
                "tags",
                Json.arr("udelezenec2025"),
                "attributes",
                attrs("kompetenca", "java"))
            .path("id")
            .asText();
  }

  private List<JsonNode> trashView(TestUser user) {
    return Json.list(user.get(acme.trash()).expect(200).json());
  }

  private long deletions() {
    return actions(history(ana, acme, anaNovak)).stream().filter("CONTACT_DELETED"::equals).count();
  }

  @Test
  @DisplayName(
      "AC-012-02 a deleted Contact leaves list, search and export and is in Trash with 30 days left")
  void ac_012_02_deleted_contact_hidden_and_in_trash() {
    moveToTrash(boris, acme, anaNovak);

    assertThat(contactPage(boris, acme, "").path("total").asInt()).isZero();
    assertThat(contactPage(boris, acme, "q=Novak").path("total").asInt()).isZero();
    Resp export = Exports.byIds(boris, acme, "en", List.of(anaNovak)).expect(200);
    assertThat(Xlsx.read(export.bytes()).rows()).isEmpty();
    List<JsonNode> trash = trashView(boris);
    assertThat(trash).hasSize(1);
    assertThat(trash.get(0).path("id").asText()).isEqualTo(anaNovak);
    assertThat(trash.get(0).path("daysLeft").asInt()).isEqualTo(30);
    assertThat(getContactStatus()).isEqualTo(404);
  }

  private int getContactStatus() {
    return boris.get(acme.contact(anaNovak)).status();
  }

  @Test
  @DisplayName("AC-012-03 a restored Contact is back with its Tag, Attribute and version number")
  void ac_012_03_restore_keeps_everything() {
    edit(ana, acme, anaNovak, "company", "Acme");
    moveToTrash(boris, acme, anaNovak);

    boris.post(acme.restore(anaNovak)).expect(200);

    JsonNode back = getContact(boris, acme, anaNovak);
    assertThat(Json.strings(back.path("tags"))).containsExactly("udelezenec2025");
    assertThat(Json.field(back.path("attributes"), "key")).containsExactly("kompetenca");
    assertThat(back.path("version").asInt()).isEqualTo(2);
    assertThat(back.path("company").asText()).isEqualTo("Acme");
    assertThat(Json.field(contactPage(boris, acme, "").path("items"), "id"))
        .containsExactly(anaNovak);
    assertThat(trashView(boris)).isEmpty();
  }

  @Test
  @DisplayName("AC-012-04 the History shows the delete and the restore by Boris with their times")
  void ac_012_04_delete_and_restore_in_history() {
    moveToTrash(boris, acme, anaNovak);
    boris.post(acme.restore(anaNovak)).expect(200);

    List<JsonNode> entries = history(ana, acme, anaNovak);

    assertThat(actions(entries)).containsSubsequence("CONTACT_RESTORED", "CONTACT_DELETED");
    for (JsonNode entry : entries.subList(0, 2)) {
      assertThat(entry.path("userName").asText()).isEqualTo("Boris Kos");
      assertThat(entry.path("occurredAt").asText()).isNotBlank();
    }
  }

  @Test
  @DisplayName("AC-012-05 a Contact in Trash cannot be edited")
  void ac_012_05_contact_in_trash_not_editable() {
    JsonNode before = getContact(ana, acme, anaNovak);
    moveToTrash(boris, acme, anaNovak);

    save(ana, acme, before, "lastName", "Kos").expectError(409, "contact.in_trash");

    boris.post(acme.restore(anaNovak)).expect(200);
    assertThat(getContact(ana, acme, anaNovak).path("lastName").asText()).isEqualTo("Novak");
  }

  @Test
  @DisplayName("AC-012-06 a Viewer may not delete, open the Trash view or restore")
  void ac_012_06_viewer_refused_three_times() {
    TestUser viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");
    String other = createContact(ana, acme, "firstName", "Cene").path("id").asText();
    moveToTrash(ana, acme, other);

    viola.post(acme.toTrash(anaNovak)).expectError(403, "auth.role_forbidden");
    viola.get(acme.trash()).expectError(403, "auth.role_forbidden");
    viola.post(acme.restore(other)).expectError(403, "auth.role_forbidden");

    assertThat(contactPage(ana, acme, "").path("total").asInt()).isEqualTo(1);
    assertThat(trashView(ana)).hasSize(1);
  }

  @Test
  @DisplayName("AC-012-07 a second delete request changes nothing and records nothing")
  void ac_012_07_repeated_delete_records_once() {
    moveToTrash(boris, acme, anaNovak);

    Resp again = boris.post(acme.toTrash(anaNovak));

    assertThat(again.status()).isBetween(200, 299);
    boris.post(acme.restore(anaNovak)).expect(200);
    assertThat(deletions()).isEqualTo(1);
  }

  @Test
  @DisplayName("AC-012-08 a Member of Beta gets not found and no data for the Trash of Acme")
  void ac_012_08_other_group_trash_not_found() {
    moveToTrash(boris, acme, anaNovak);
    TestUser cene = user("Cene Zupan", "cene@acme.com");
    group(cene, "Beta");

    Resp answer = cene.get(acme.trash()).expect(404);

    assertThat(answer.body()).doesNotContain("Novak");
  }

  @Test
  @DisplayName("EX-012-01 a restore after 29 days 23 hours in Trash works")
  void ex_012_01_restore_just_before_30_days() {
    moveToTrash(boris, acme, anaNovak);
    clock.advance(Duration.ofDays(29).plusHours(23));

    boris.post(acme.restore(anaNovak)).expect(200);

    assertThat(getContact(boris, acme, anaNovak).path("lastName").asText()).isEqualTo("Novak");
  }

  @Test
  @DisplayName("EX-012-02 a restore at 30 days 0 minutes in Trash is not found")
  void ex_012_02_restore_at_30_days_not_found() {
    moveToTrash(boris, acme, anaNovak);
    clock.advance(Duration.ofDays(30));

    boris.post(acme.restore(anaNovak)).expect(404);

    assertThat(boris.get(acme.contact(anaNovak)).status()).isEqualTo(404);
  }

  @Test
  @DisplayName("EX-012-04 two Editors deleting at once record one deletion")
  void ex_012_04_concurrent_deletes_record_once() throws Exception {
    TestUser cene = member(acme, ana, "Cene Zupan", "cene@acme.com", "EDITOR");

    List<Resp> answers =
        atOnce(() -> boris.post(acme.toTrash(anaNovak)), () -> cene.post(acme.toTrash(anaNovak)));

    assertThat(answers).allMatch(a -> a.status() >= 200 && a.status() < 300);
    assertThat(trashView(ana)).hasSize(1);
    ana.post(acme.restore(anaNovak)).expect(200);
    assertThat(deletions()).isEqualTo(1);
  }

  @Test
  @DisplayName("EX-012-05 saving a Contact another Member just moved to Trash is refused")
  void ex_012_05_save_after_other_deleted_refused() {
    JsonNode borisOpened = getContact(boris, acme, anaNovak);
    moveToTrash(ana, acme, anaNovak);

    Resp answer = save(boris, acme, borisOpened, "company", "Beta");

    assertThat(answer.status()).isEqualTo(409);
    assertThat(answer.code()).isEqualTo("contact.in_trash");
    ana.post(acme.restore(anaNovak)).expect(200);
    assertThat(getContact(ana, acme, anaNovak).path("company").asText("")).isEmpty();
  }

  @Test
  @DisplayName(
      "EX-012-06 a restored Contact whose e-mail another Contact now has is restored; both exist")
  void ex_012_06_restore_with_duplicate_email() {
    moveToTrash(boris, acme, anaNovak);
    createContact(ana, acme, "firstName", "Druga", "email", "ana.novak@acme.com");

    boris.post(acme.restore(anaNovak)).expect(200);

    assertThat(contactPage(ana, acme, "").path("total").asInt()).isEqualTo(2);
  }

  @Test
  @DisplayName("EX-012-07 the Trash view with no Contacts is empty")
  void ex_012_07_empty_trash() {
    assertThat(trashView(boris)).isEmpty();
  }
}
