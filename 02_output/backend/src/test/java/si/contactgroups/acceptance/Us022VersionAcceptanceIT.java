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

/** US-022 View and compare earlier Versions of a Contact. */
class Us022VersionAcceptanceIT extends AcceptanceTestBase {

  private TestUser ana;
  private TestUser boris;
  private Grp acme;
  private String anaNovak;

  @BeforeEach
  void threeVersions() {
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
                "company",
                "Acme",
                "attributes",
                attrs("kompetenca", "java"))
            .path("id")
            .asText();
  }

  private void toV3() {
    edit(boris, acme, anaNovak, "city", "Celje");
    edit(boris, acme, anaNovak, "city", null, "company", "Beta");
  }

  private List<JsonNode> versions(TestUser user) {
    return Json.list(user.get(acme.versions(anaNovak)).expect(200).json());
  }

  private List<JsonNode> differences(int number) {
    return Json.list(
        ana.get(acme.version(anaNovak, number) + "/compare")
            .expect(200)
            .json()
            .path("differences"));
  }

  @Test
  @DisplayName("AC-022-01 the Versions tab lists v3, v2, v1 with time and user")
  void ac_022_01_versions_newest_first() {
    toV3();

    List<JsonNode> list = versions(boris);

    assertThat(list.stream().map(v -> v.path("number").asInt())).containsExactly(3, 2, 1);
    assertThat(list.get(0).path("userName").asText()).isEqualTo("Boris Kos");
    assertThat(list.get(2).path("userName").asText()).isEqualTo("Ana Novak");
    assertThat(list).allMatch(v -> !v.path("createdAt").asText().isBlank());
  }

  @Test
  @DisplayName("AC-022-02 v1 shows company Acme and the Tags and Attributes of v1")
  void ac_022_02_open_earlier_version() {
    toV3();

    JsonNode v1 = boris.get(acme.version(anaNovak, 1)).expect(200).json();

    assertThat(v1.path("number").asInt()).isEqualTo(1);
    assertThat(v1.path("content").path("company").asText()).isEqualTo("Acme");
    assertThat(v1.path("content").path("tags")).isEmpty();
    assertThat(Json.field(v1.path("content").path("attributes"), "value")).containsExactly("java");
  }

  @Test
  @DisplayName(
      "AC-022-03 comparing v1 with the current v3 lists company Acme → Beta and nothing else")
  void ac_022_03_compare_with_current() {
    toV3();

    List<JsonNode> diff = differences(1);

    assertThat(diff).hasSize(1);
    assertThat(diff.get(0).path("kind").asText()).isEqualTo("field");
    assertThat(diff.get(0).path("name").asText()).isEqualTo("company");
    assertThat(diff.get(0).path("versionValue").asText()).isEqualTo("Acme");
    assertThat(diff.get(0).path("currentValue").asText()).isEqualTo("Beta");
  }

  @Test
  @DisplayName("AC-022-04 a Tag added between v1 and v2 is shown as added")
  void ac_022_04_tag_added() {
    edit(ana, acme, anaNovak, "tags", Json.arr("gost"));

    List<JsonNode> diff = differences(1);

    assertThat(diff).hasSize(1);
    assertThat(diff.get(0).path("kind").asText()).isEqualTo("tag");
    assertThat(diff.get(0).path("name").asText()).isEqualTo("gost");
    assertThat(
            diff.get(0).path("versionValue").isNull()
                || diff.get(0).path("versionValue").isMissingNode())
        .isTrue();
  }

  @Test
  @DisplayName("AC-022-05 a Viewer's request for the Versions is refused")
  void ac_022_05_viewer_refused() {
    TestUser viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");

    viola.get(acme.versions(anaNovak)).expectError(403, "auth.role_forbidden");
    viola.get(acme.version(anaNovak, 1)).expectError(403, "auth.role_forbidden");
    viola.get(acme.version(anaNovak, 1) + "/compare").expectError(403, "auth.role_forbidden");
  }

  @Test
  @DisplayName("AC-022-06 any request to change or delete a Version is refused")
  void ac_022_06_versions_cannot_be_changed() {
    JsonNode before = ana.get(acme.version(anaNovak, 1)).expect(200).json();

    List<Resp> answers =
        List.of(
            ana.put(acme.version(anaNovak, 1), Json.obj("content", Json.obj("company", "X"))),
            ana.patch(acme.version(anaNovak, 1), Json.obj("content", Json.obj("company", "X"))),
            ana.delete(acme.version(anaNovak, 1)),
            ana.delete(acme.versions(anaNovak)));

    assertThat(answers).allMatch(a -> a.status() >= 400 && a.status() < 500);
    assertThat(ana.get(acme.version(anaNovak, 1)).expect(200).json()).isEqualTo(before);
  }

  @Test
  @DisplayName("AC-022-07 the Versions of a Contact deleted forever no longer exist")
  void ac_022_07_versions_deleted_with_contact() {
    toV3();
    moveToTrash(ana, acme, anaNovak);

    ana.delete(acme.trashed(anaNovak)).expect(204);

    ana.get(acme.versions(anaNovak)).expect(404);
    ana.get(acme.version(anaNovak, 1)).expect(404);
    assertThat(Db.countWhere("contact_version", "contact_id", anaNovak)).isZero();
  }

  @Test
  @DisplayName(
      "EX-022-01 a Contact with only v1 has one Version and no difference to the current one")
  void ex_022_01_only_v1() {
    assertThat(versions(ana)).hasSize(1);
    assertThat(differences(1)).isEmpty();
  }

  @Test
  @DisplayName("EX-022-02 comparing the current Version with itself gives no differences")
  void ex_022_02_current_with_itself() {
    toV3();

    assertThat(differences(3)).isEmpty();
  }

  @Test
  @DisplayName("EX-022-03 a Version number that does not exist is not found")
  void ex_022_03_missing_version() {
    ana.get(acme.version(anaNovak, 99)).expect(404);
    ana.get(acme.version(anaNovak, 99) + "/compare").expect(404);
  }

  @Test
  @DisplayName("EX-022-04 the Versions of a Contact in Trash are visible after restore only")
  void ex_022_04_versions_of_contact_in_trash() {
    toV3();
    moveToTrash(ana, acme, anaNovak);

    ana.get(acme.versions(anaNovak)).expect(404);
    ana.post(acme.restore(anaNovak)).expect(200);

    assertThat(versions(ana)).hasSize(3);
  }

  @Test
  @DisplayName("EX-022-05 a save that changed nothing makes no new Version")
  void ex_022_05_no_change_no_version() {
    save(ana, acme, getContact(ana, acme, anaNovak)).expect(200);

    assertThat(versions(ana)).hasSize(1);
  }
}
