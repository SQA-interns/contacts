package si.contactgroups.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import si.contactgroups.acceptance.support.AcceptanceTestBase;
import si.contactgroups.acceptance.support.Db;
import si.contactgroups.acceptance.support.Grp;
import si.contactgroups.acceptance.support.Json;
import si.contactgroups.acceptance.support.TestUser;

/** US-006 Rename and delete a Tag ({@code /groups/{g}/tags}). */
class Us006TagListAcceptanceIT extends AcceptanceTestBase {

  private TestUser ana;
  private Grp acme;

  @BeforeEach
  void anaOwnsAcme() {
    ana = user("Ana Novak", "ana@acme.com");
    acme = group(ana, "Acme");
  }

  private List<String> contactsWith(String tag, int count) {
    List<String> ids = new ArrayList<>();
    for (int i = 1; i <= count; i++) {
      ids.add(
          createContact(ana, acme, "firstName", "Oseba" + i, "tags", Json.arr(tag))
              .path("id")
              .asText());
    }
    return ids;
  }

  private List<String> tagsOf(String contactId) {
    return Json.strings(getContact(ana, acme, contactId).path("tags"));
  }

  private int countOf(String name) {
    return tagList(ana, acme).stream()
        .filter(t -> t.path("name").asText().equals(name))
        .findFirst()
        .orElseThrow()
        .path("contactCount")
        .asInt();
  }

  private List<String> groupTagNames() {
    return tagList(ana, acme).stream().map(t -> t.path("name").asText()).toList();
  }

  @Test
  @DisplayName("AC-006-01 renaming udelezenec2025 to udelezenec2026 changes it on all 3 Contacts")
  void ac_006_01_rename_on_all_contacts() {
    List<String> ids = contactsWith("udelezenec2025", 3);

    var renamed =
        ana.patch(acme.tag(tagId(ana, acme, "udelezenec2025")), Json.obj("name", "udelezenec2026"))
            .expect(200)
            .json();

    assertThat(renamed.path("name").asText()).isEqualTo("udelezenec2026");
    for (String id : ids) {
      assertThat(tagsOf(id)).containsExactly("udelezenec2026");
    }
    assertThat(countOf("udelezenec2026")).isEqualTo(3);
    assertThat(groupTagNames()).containsExactly("udelezenec2026");
  }

  @Test
  @DisplayName("AC-006-02 renaming gost to UDELEZENEC2025 is refused; nothing changes")
  void ac_006_02_rename_to_existing_name_refused() {
    String guest = contactsWith("gost", 1).get(0);
    contactsWith("udelezenec2025", 1);

    ana.patch(acme.tag(tagId(ana, acme, "gost")), Json.obj("name", "UDELEZENEC2025"))
        .expectError(409, "tag.name_taken");

    assertThat(groupTagNames()).containsExactlyInAnyOrder("gost", "udelezenec2025");
    assertThat(tagsOf(guest)).containsExactly("gost");
  }

  @Test
  @DisplayName("AC-006-03 renaming gost to Gost changes the spelling")
  void ac_006_03_case_only_rename() {
    String guest = contactsWith("gost", 1).get(0);

    ana.patch(acme.tag(tagId(ana, acme, "gost")), Json.obj("name", "Gost")).expect(200);

    assertThat(groupTagNames()).containsExactly("Gost");
    assertThat(tagsOf(guest)).containsExactly("Gost");
  }

  @Test
  @DisplayName("AC-006-04 deleting gost (5 contacts) removes it from all 5; the Contacts stay")
  void ac_006_04_delete_tag_keeps_contacts() {
    List<String> ids = contactsWith("gost", 5);
    assertThat(countOf("gost")).isEqualTo(5);

    ana.delete(acme.tag(tagId(ana, acme, "gost"))).expect(204);

    assertThat(groupTagNames()).isEmpty();
    for (String id : ids) {
      assertThat(tagsOf(id)).isEmpty();
    }
    assertThat(contactPage(ana, acme, "").path("total").asInt()).isEqualTo(5);
  }

  @Test
  @DisplayName("AC-006-06 a rename writes one History entry per Contact and keeps version 4")
  void ac_006_06_rename_history_without_version_change() {
    List<String> ids = contactsWith("gost", 2);
    String four = ids.get(0);
    edit(ana, acme, four, "city", "Kranj");
    edit(ana, acme, four, "city", "Celje");
    edit(ana, acme, four, "city", "Koper");
    assertThat(getContact(ana, acme, four).path("version").asInt()).isEqualTo(4);
    int before0 = history(ana, acme, ids.get(0)).size();
    int before1 = history(ana, acme, ids.get(1)).size();

    ana.patch(acme.tag(tagId(ana, acme, "gost")), Json.obj("name", "obiskovalec")).expect(200);

    List<JsonNode> h0 = history(ana, acme, ids.get(0));
    List<JsonNode> h1 = history(ana, acme, ids.get(1));
    assertThat(h0).hasSize(before0 + 1);
    assertThat(h1).hasSize(before1 + 1);
    assertThat(h0.get(0).path("action").asText()).isEqualTo("TAG_RENAMED");
    assertThat(h0.get(0).path("details").toString()).contains("gost").contains("obiskovalec");
    assertThat(getContact(ana, acme, four).path("version").asInt()).isEqualTo(4);
    assertThat(getContact(ana, acme, ids.get(1)).path("version").asInt()).isEqualTo(1);
  }

  @Test
  @DisplayName("AC-006-07 a Viewer's rename request is refused; nothing changes")
  void ac_006_07_viewer_cannot_rename() {
    contactsWith("gost", 1);
    String gost = tagId(ana, acme, "gost");
    TestUser viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");

    viola
        .patch(acme.tag(gost), Json.obj("name", "obiskovalec"))
        .expectError(403, "auth.role_forbidden");
    viola.delete(acme.tag(gost)).expectError(403, "auth.role_forbidden");

    assertThat(groupTagNames()).containsExactly("gost");
  }

  @ParameterizedTest(name = "EX-006-01 new name of {0} characters: accepted = {1}")
  @CsvSource({"50, true", "51, false"})
  void ex_006_01_new_name_length(int length, boolean accepted) {
    contactsWith("gost", 1);
    String name = chars(length, 'g');

    var answer = ana.patch(acme.tag(tagId(ana, acme, "gost")), Json.obj("name", name));

    assertThat(answer.status()).isEqualTo(accepted ? 200 : 422);
    assertThat(groupTagNames()).containsExactly(accepted ? name : "gost");
  }

  @Test
  @DisplayName("EX-006-02 a new name with a space is refused")
  void ex_006_02_name_with_space_refused() {
    contactsWith("gost", 1);

    ana.patch(acme.tag(tagId(ana, acme, "gost")), Json.obj("name", "nov gost")).expect(422);

    assertThat(groupTagNames()).containsExactly("gost");
  }

  @Test
  @DisplayName("EX-006-03 a Tag no Contact carries is deleted without History entries")
  void ex_006_03_unused_tag_deleted_without_history() {
    String id = contactsWith("gost", 1).get(0);
    edit(ana, acme, id, "tags", Json.arr());
    int entries = activity(ana, acme, "").size();

    ana.delete(acme.tag(tagId(ana, acme, "gost"))).expect(204);

    assertThat(groupTagNames()).isEmpty();
    assertThat(activity(ana, acme, "")).hasSize(entries);
  }

  @Test
  @DisplayName("EX-006-04 a Contact in Trash loses the deleted Tag and is restored without it")
  void ex_006_04_contact_in_trash_loses_tag() {
    String id = contactsWith("gost", 1).get(0);
    moveToTrash(ana, acme, id);

    ana.delete(acme.tag(tagId(ana, acme, "gost"))).expect(204);
    ana.post(acme.restore(id)).expect(200);

    assertThat(tagsOf(id)).isEmpty();
  }

  @Test
  @DisplayName("EX-006-05 renaming to the same spelling changes nothing and writes no History")
  void ex_006_05_same_spelling_no_history() {
    String id = contactsWith("gost", 1).get(0);
    int entries = history(ana, acme, id).size();

    var answer = ana.patch(acme.tag(tagId(ana, acme, "gost")), Json.obj("name", "gost"));

    assertThat(answer.status()).isEqualTo(200);
    assertThat(groupTagNames()).containsExactly("gost");
    assertThat(history(ana, acme, id)).hasSize(entries);
  }

  @Test
  @DisplayName("EX-006-06 a Tag of another Group requested by id is not found")
  void ex_006_06_tag_of_other_group_not_found() {
    Grp beta = group(ana, "Beta");
    createContact(ana, beta, "firstName", "Cene", "tags", Json.arr("gost"));
    String betaTag = tagId(ana, beta, "gost");

    ana.patch(acme.tag(betaTag), Json.obj("name", "obiskovalec")).expect(404);
    ana.delete(acme.tag(betaTag)).expect(404);
    ana.patch(acme.tag(Db.randomId()), Json.obj("name", "obiskovalec")).expect(404);

    assertThat(tagList(ana, beta).get(0).path("name").asText()).isEqualTo("gost");
  }
}
