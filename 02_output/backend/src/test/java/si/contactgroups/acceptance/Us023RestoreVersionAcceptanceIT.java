package si.contactgroups.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
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

/** US-023 Restore an earlier Version of a Contact. */
class Us023RestoreVersionAcceptanceIT extends AcceptanceTestBase {

  private TestUser ana;
  private TestUser boris;
  private Grp acme;

  @BeforeEach
  void anaAndBorisInAcme() {
    ana = user("Ana Novak", "ana@acme.com");
    acme = group(ana, "Acme");
    boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
  }

  /** Ana Novak with v1 company Acme, v2 Beta, v3 Cezar. */
  private String threeCompanies() {
    String id =
        createContact(ana, acme, "firstName", "Ana", "lastName", "Novak", "company", "Acme")
            .path("id")
            .asText();
    edit(ana, acme, id, "company", "Beta");
    edit(ana, acme, id, "company", "Cezar");
    return id;
  }

  private Resp restore(TestUser user, String id, int number, int seenVersion) {
    return user.post(acme.version(id, number) + "/restore", Json.obj("version", seenVersion));
  }

  private JsonNode content(String id, int number) {
    return ana.get(acme.version(id, number)).expect(200).json().path("content");
  }

  @Test
  @DisplayName("AC-023-01 restoring v1 gives company Acme as v4; v1 to v3 still exist")
  void ac_023_01_restore_as_new_version() {
    String id = threeCompanies();

    var restored = restore(boris, id, 1, 3).expect(200).json();

    assertThat(restored.path("company").asText()).isEqualTo("Acme");
    assertThat(restored.path("version").asInt()).isEqualTo(4);
    assertThat(getContact(ana, acme, id).path("company").asText()).isEqualTo("Acme");
    assertThat(content(id, 1).path("company").asText()).isEqualTo("Acme");
    assertThat(content(id, 2).path("company").asText()).isEqualTo("Beta");
    assertThat(content(id, 3).path("company").asText()).isEqualTo("Cezar");
    assertThat(content(id, 4).path("company").asText()).isEqualTo("Acme");
  }

  @Test
  @DisplayName("AC-023-02 the History shows 'restored version 1' with company Cezar → Acme")
  void ac_023_02_restore_in_history() {
    String id = threeCompanies();

    restore(boris, id, 1, 3).expect(200);

    JsonNode entry = history(ana, acme, id).get(0);
    assertThat(entry.path("action").asText()).isEqualTo("VERSION_RESTORED");
    assertThat(entry.path("userName").asText()).isEqualTo("Boris Kos");
    assertThat(entry.path("details").toString()).contains("1").contains("Cezar").contains("Acme");
  }

  @Test
  @DisplayName("AC-023-03 a v1 with only the e-mail ana@x.si is restored")
  void ac_023_03_valid_content_restored() {
    String id = createContact(ana, acme, "email", "ana@x.si").path("id").asText();
    edit(ana, acme, id, "firstName", "Ana", "email", null);

    var restored = restore(ana, id, 1, 2).expect(200).json();

    assertThat(restored.path("email").asText()).isEqualTo("ana@x.si");
    assertThat(restored.path("firstName").asText("")).isEmpty();
    assertThat(restored.path("version").asInt()).isEqualTo(3);
  }

  @Test
  @DisplayName(
      "AC-023-04 a Version that breaks BR-06 (an invalid e-mail of a past rule) is not restored")
  void ac_023_04_invalid_content_refused() {
    String id =
        createContact(ana, acme, "firstName", "Ana", "email", "ana@x.si").path("id").asText();
    edit(ana, acme, id, "email", "ana@y.si");
    // a past rule accepted 'ana@': the stored Version 1 holds it (fixture through the schema)
    Db.replaceInVersion(id, 1, "\"ana@x.si\"", "\"ana@\"");

    Resp answer = restore(ana, id, 1, 2).expect(422);

    assertThat(answer.errorCodes()).contains("validation.email_invalid");
    JsonNode stored = getContact(ana, acme, id);
    assertThat(stored.path("email").asText()).isEqualTo("ana@y.si");
    assertThat(stored.path("version").asInt()).isEqualTo(2);
  }

  @Test
  @DisplayName("AC-023-05 Ana's restore after Boris saved v4 is refused as changed by someone else")
  void ac_023_05_stale_version_refused() {
    String id = threeCompanies();
    edit(boris, acme, id, "city", "Celje");

    restore(ana, id, 1, 3).expectError(409, "contact.changed_by_someone_else");

    JsonNode stored = getContact(ana, acme, id);
    assertThat(stored.path("company").asText()).isEqualTo("Cezar");
    assertThat(stored.path("version").asInt()).isEqualTo(4);
  }

  @Test
  @DisplayName("AC-023-06 restoring v1 with the deleted Tag gost creates gost again and puts it on")
  void ac_023_06_deleted_tag_created_again() {
    String id =
        createContact(ana, acme, "firstName", "Ana", "tags", Json.arr("gost")).path("id").asText();
    edit(ana, acme, id, "company", "Acme");
    ana.delete(acme.tag(tagId(ana, acme, "gost"))).expect(204);

    restore(ana, id, 1, 2).expect(200);

    assertThat(Json.strings(getContact(ana, acme, id).path("tags"))).containsExactly("gost");
    assertThat(tagList(ana, acme).stream().map(t -> t.path("name").asText()))
        .containsExactly("gost");
  }

  @Test
  @DisplayName("AC-023-07 a Viewer's restore is refused")
  void ac_023_07_viewer_refused() {
    String id = threeCompanies();
    TestUser viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");

    restore(viola, id, 1, 3).expectError(403, "auth.role_forbidden");

    assertThat(getContact(ana, acme, id).path("version").asInt()).isEqualTo(3);
  }

  @Test
  @DisplayName("EX-023-01 restoring the current Version changes nothing and makes no new Version")
  void ex_023_01_restore_current() {
    String id = threeCompanies();

    restore(ana, id, 3, 3);

    assertThat(getContact(ana, acme, id).path("version").asInt()).isEqualTo(3);
    assertThat(Json.list(ana.get(acme.versions(id)).expect(200).json())).hasSize(3);
  }

  @Test
  @DisplayName("EX-023-02 restoring a Version of a Contact in Trash is refused")
  void ex_023_02_contact_in_trash() {
    String id = threeCompanies();
    moveToTrash(ana, acme, id);

    assertThat(restore(ana, id, 1, 3).status()).isBetween(400, 499);

    ana.post(acme.restore(id)).expect(200);
    assertThat(getContact(ana, acme, id).path("company").asText()).isEqualTo("Cezar");
  }

  @Test
  @DisplayName("EX-023-03 a Version number that does not exist is not found")
  void ex_023_03_missing_version() {
    String id = threeCompanies();

    restore(ana, id, 9, 3).expect(404);
  }

  @Test
  @DisplayName("EX-023-04 a Version with 50 Tags that the Group has is restored")
  void ex_023_04_fifty_tags() {
    List<String> tags = new ArrayList<>();
    for (int i = 1; i <= 50; i++) {
      tags.add("znacka" + i);
    }
    String id = createContact(ana, acme, "firstName", "Ana", "tags", tags).path("id").asText();
    edit(ana, acme, id, "tags", Json.arr());

    restore(ana, id, 1, 2).expect(200);

    assertThat(Json.strings(getContact(ana, acme, id).path("tags"))).hasSize(50);
  }
}
