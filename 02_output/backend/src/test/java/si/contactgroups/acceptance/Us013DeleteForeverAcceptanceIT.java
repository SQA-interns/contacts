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

/**
 * US-013 Delete a Contact forever (by the Owner, and by the clean-up job of BR-04, which runs every
 * second in this test context). The deleted Contact is Mojca Zupan, so that her data cannot be
 * confused with the name of the acting Owner Ana Novak.
 */
class Us013DeleteForeverAcceptanceIT extends AcceptanceTestBase {

  private TestUser ana;
  private Grp acme;
  private String mojca;

  @BeforeEach
  void mojcaInAcme() {
    ana = user("Ana Novak", "ana@acme.com");
    acme = group(ana, "Acme");
    mojca =
        createContact(
                ana,
                acme,
                "firstName",
                "Mojca",
                "lastName",
                "Zupan",
                "email",
                "mojca@zupan.si",
                "company",
                "Zupanova d.o.o.",
                "tags",
                Json.arr("udelezenec2025"),
                "attributes",
                attrs("kompetenca", "java"))
            .path("id")
            .asText();
  }

  private List<String> trashIds() {
    return Json.field(ana.get(acme.trash()).expect(200).json(), "id");
  }

  private static void assertNoMojcaData(String text) {
    assertThat(text)
        .doesNotContain("Mojca")
        .doesNotContain("Zupan")
        .doesNotContain("mojca@zupan.si");
  }

  @Test
  @DisplayName(
      "AC-013-01 the Owner deletes a Contact 3 days in Trash forever; Trash no longer shows it")
  void ac_013_01_owner_deletes_forever() {
    moveToTrash(ana, acme, mojca);
    clock.advance(Duration.ofDays(3));

    ana.delete(acme.trashed(mojca)).expect(204);

    assertThat(trashIds()).doesNotContain(mojca);
    ana.get(acme.contact(mojca)).expect(404);
  }

  @Test
  @DisplayName("AC-013-03 an Editor's delete-forever call is refused; the Contact stays in Trash")
  void ac_013_03_editor_refused() {
    TestUser boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
    moveToTrash(ana, acme, mojca);

    boris.delete(acme.trashed(mojca)).expectError(403, "auth.role_forbidden");

    assertThat(trashIds()).containsExactly(mojca);
  }

  @Test
  @DisplayName(
      "AC-013-04 after 30 days and 1 hour in Trash the clean-up removes Contact, Attributes, Versions")
  void ac_013_04_clean_up_after_30_days() {
    moveToTrash(ana, acme, mojca);
    assertThat(Db.countWhere("contact", "id", mojca)).isEqualTo(1);

    clock.advance(Duration.ofDays(30).plusHours(1));

    awaitCleanUp(() -> Db.countWhere("contact", "id", mojca) == 0);
    assertThat(Db.countWhere("attribute", "contact_id", mojca)).isZero();
    assertThat(Db.countWhere("contact_version", "contact_id", mojca)).isZero();
    assertThat(Db.countWhere("contact_tag", "contact_id", mojca)).isZero();
  }

  @Test
  @DisplayName("AC-013-05 a restore or read of a Contact deleted forever is not found")
  void ac_013_05_after_forever_not_found() {
    moveToTrash(ana, acme, mojca);
    ana.delete(acme.trashed(mojca)).expect(204);

    ana.post(acme.restore(mojca)).expect(404);
    ana.get(acme.contact(mojca)).expect(404);
    ana.get(acme.history(mojca)).expect(404);
  }

  @Test
  @DisplayName(
      "AC-013-06 the 'deleted forever' entry has no name, e-mail or other field of the Contact")
  void ac_013_06_forever_entry_without_personal_data() {
    moveToTrash(ana, acme, mojca);
    ana.delete(acme.trashed(mojca)).expect(204);

    List<JsonNode> entries = activity(ana, acme, "action=CONTACT_DELETED_FOREVER");

    assertThat(entries).hasSize(1);
    assertThat(entries.get(0).path("userName").asText()).isEqualTo("Ana Novak");
    assertNoMojcaData(entries.get(0).toString());
  }

  @Test
  @DisplayName("AC-013-07 the Tag of the deleted Contact still exists")
  void ac_013_07_tags_kept() {
    moveToTrash(ana, acme, mojca);
    ana.delete(acme.trashed(mojca)).expect(204);

    assertThat(tagList(ana, acme).stream().map(t -> t.path("name").asText()))
        .containsExactly("udelezenec2025");
  }

  @Test
  @DisplayName(
      "AC-013-08 earlier entries stay with time, user and action; their values read 'removed'")
  void ac_013_08_earlier_entries_values_removed() {
    edit(ana, acme, mojca, "lastName", "Kos");
    List<JsonNode> before = activity(ana, acme, "");
    moveToTrash(ana, acme, mojca);

    ana.delete(acme.trashed(mojca)).expect(204);

    List<JsonNode> after = activity(ana, acme, "");
    JsonNode edited =
        after.stream()
            .filter(e -> e.path("action").asText().equals("CONTACT_EDITED"))
            .findFirst()
            .orElseThrow();
    assertThat(edited.path("occurredAt").asText()).isNotBlank();
    assertThat(edited.path("userName").asText()).isEqualTo("Ana Novak");
    assertThat(edited.path("details").toString())
        .contains("removed")
        .doesNotContain("Zupan")
        .doesNotContain("Kos");
    for (JsonNode entry : after) {
      assertNoMojcaData(entry.path("details").toString());
    }
    assertThat(actions(after)).containsAll(actions(before));
  }

  @Test
  @DisplayName(
      "EX-013-01 delete forever of a Contact not in Trash is refused; the Contact is unchanged")
  void ex_013_01_not_in_trash_refused() {
    Resp answer = ana.delete(acme.trashed(mojca));

    assertThat(answer.status()).isBetween(400, 499);
    assertThat(getContact(ana, acme, mojca).path("lastName").asText()).isEqualTo("Zupan");
  }

  @Test
  @DisplayName(
      "EX-013-02 exactly 30 days in Trash counts as expired and is removed by the next clean-up")
  void ex_013_02_exactly_30_days_removed() {
    moveToTrash(ana, acme, mojca);

    clock.advance(Duration.ofDays(30));

    awaitCleanUp(() -> Db.countWhere("contact", "id", mojca) == 0);
  }

  @Test
  @DisplayName("EX-013-03 after 29 days 23 hours the Contact is still in Trash")
  void ex_013_03_before_30_days_kept() {
    moveToTrash(ana, acme, mojca);

    clock.advance(Duration.ofDays(29).plusHours(23));
    letCleanUpRun();

    assertThat(trashIds()).containsExactly(mojca);
  }

  @Test
  @DisplayName("EX-013-04 two Owners confirming at once: one deletion, the other gets not found")
  void ex_013_04_concurrent_forever() throws Exception {
    TestUser cene = member(acme, ana, "Cene Zupan", "cene@acme.com", "OWNER");
    moveToTrash(ana, acme, mojca);

    List<Resp> answers =
        atOnce(() -> ana.delete(acme.trashed(mojca)), () -> cene.delete(acme.trashed(mojca)));

    assertThat(answers.stream().map(Resp::status)).containsExactlyInAnyOrder(204, 404);
    assertThat(activity(ana, acme, "action=CONTACT_DELETED_FOREVER")).hasSize(1);
  }

  @Test
  @DisplayName("EX-013-06 in a merge entry only the deleted Contact's values read 'removed'")
  void ex_013_06_merge_entry_keeps_other_contact_values() {
    String survivor =
        createContact(
                ana,
                acme,
                "firstName",
                "Ohranjena",
                "lastName",
                "Oseba",
                "email",
                "skupna@acme.com")
            .path("id")
            .asText();
    String merged =
        createContact(
                ana, acme, "firstName", "Brisana", "lastName", "Oseba", "email", "skupna@acme.com")
            .path("id")
            .asText();
    JsonNode s = getContact(ana, acme, survivor);
    JsonNode m = getContact(ana, acme, merged);
    ana.post(
            acme.merges(),
            Json.obj(
                "survivorId", survivor,
                "contacts",
                    Json.arr(
                        Json.obj("id", survivor, "version", s.path("version").asInt()),
                        Json.obj("id", merged, "version", m.path("version").asInt())),
                "fields",
                    Json.obj(
                        "firstName", "Ohranjena", "lastName", "Oseba", "email", "skupna@acme.com"),
                "attributes", Json.arr()))
        .expect(200);

    ana.delete(acme.trashed(merged)).expect(204);

    JsonNode entry = activity(ana, acme, "action=CONTACTS_MERGED").get(0);
    assertThat(entry.path("details").toString()).contains("Ohranjena").doesNotContain("Brisana");
  }

  @Test
  @DisplayName(
      "EX-013-05 a Contact in Trash of a deleted Group goes with the Group after 30 days (US-020)")
  void ex_013_05_contact_in_trash_of_deleted_group() {
    moveToTrash(ana, acme, mojca);
    ana.post(acme.toGroupTrash(), Json.obj("name", "Acme")).expect(204);

    clock.advance(Duration.ofDays(30).plusHours(1));

    awaitCleanUp(() -> Db.countWhere("contact_group", "id", acme.id()) == 0);
    assertThat(Db.countWhere("contact", "id", mojca)).isZero();
  }
}
