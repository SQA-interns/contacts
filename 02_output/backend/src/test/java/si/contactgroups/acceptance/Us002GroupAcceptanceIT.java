package si.contactgroups.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.contactgroups.acceptance.support.AcceptanceTestBase;
import si.contactgroups.acceptance.support.Grp;
import si.contactgroups.acceptance.support.Json;
import si.contactgroups.acceptance.support.TestUser;

/** US-002 Create a Group and see my Groups (through {@code /groups}). */
class Us002GroupAcceptanceIT extends AcceptanceTestBase {

  private List<JsonNode> groupsOf(TestUser user) {
    return Json.list(user.get("/groups").expect(200).json());
  }

  @Test
  @DisplayName("AC-002-01 the creator of Acme is its Owner and sees it with Role Owner")
  void ac_002_01_creator_is_owner() {
    TestUser ana = user("Ana Novak", "ana@acme.com");
    assertThat(groupsOf(ana)).isEmpty();

    var created = ana.post("/groups", Json.obj("name", "Acme")).expect(201).json();

    assertThat(created.path("name").asText()).isEqualTo("Acme");
    assertThat(created.path("role").asText()).isEqualTo("OWNER");
    List<JsonNode> groups = groupsOf(ana);
    assertThat(groups).hasSize(1);
    assertThat(groups.get(0).path("name").asText()).isEqualTo("Acme");
    assertThat(groups.get(0).path("role").asText()).isEqualTo("OWNER");
  }

  @Test
  @DisplayName("AC-002-02 the name '  Acme  ' is stored trimmed as 'Acme'")
  void ac_002_02_name_is_trimmed() {
    TestUser ana = user("Ana Novak", "ana@acme.com");

    var created = ana.post("/groups", Json.obj("name", "  Acme  ")).expect(201).json();

    assertThat(created.path("name").asText()).isEqualTo("Acme");
    assertThat(Json.field(Json.MAPPER.valueToTree(groupsOf(ana)), "name")).containsExactly("Acme");
  }

  @Test
  @DisplayName(
      "AC-002-03 ACME is refused when Ana owns Acme: 'You already have a group with this name'")
  void ac_002_03_same_name_other_case_refused() {
    TestUser ana = user("Ana Novak", "ana@acme.com");
    group(ana, "Acme");

    ana.post("/groups", Json.obj("name", "ACME")).expectError(409, "group.name_taken");

    assertThat(groupsOf(ana)).hasSize(1);
  }

  @Test
  @DisplayName("AC-002-04 two Owners may both have a Group named Acme; each sees only their own")
  void ac_002_04_same_name_different_owners() {
    TestUser ana = user("Ana Novak", "ana@acme.com");
    TestUser boris = user("Boris Kos", "boris@acme.com");
    Grp anas = group(ana, "Acme");

    Grp boriss = group(boris, "Acme");

    assertThat(Json.field(Json.MAPPER.valueToTree(groupsOf(ana)), "id")).containsExactly(anas.id());
    assertThat(Json.field(Json.MAPPER.valueToTree(groupsOf(boris)), "id"))
        .containsExactly(boriss.id());
    assertThat(anas.id()).isNotEqualTo(boriss.id());
  }

  @Test
  @DisplayName("AC-002-05 an empty name creates no Group")
  void ac_002_05_empty_name_refused() {
    TestUser ana = user("Ana Novak", "ana@acme.com");

    var answer = ana.post("/groups", Json.obj("name", "")).expect(422);

    assertThat(answer.errorFields()).contains("name");
    assertThat(groupsOf(ana)).isEmpty();
  }

  @Test
  @DisplayName("AC-002-06 Boris sees only Beta; Acme of Ana is not shown to him")
  void ac_002_06_only_own_groups_listed() {
    TestUser ana = user("Ana Novak", "ana@acme.com");
    TestUser boris = user("Boris Kos", "boris@acme.com");
    group(ana, "Acme");
    Grp beta = group(boris, "Beta");

    List<JsonNode> groups = groupsOf(boris);

    assertThat(Json.field(Json.MAPPER.valueToTree(groups), "id")).containsExactly(beta.id());
    assertThat(boris.get("/groups").body()).doesNotContain("Acme");
  }

  @Test
  @DisplayName("AC-002-07 a User with no Group gets an empty Group list (the empty state)")
  void ac_002_07_no_group_empty_list() {
    TestUser ana = user("Ana Novak", "ana@acme.com");

    assertThat(groupsOf(ana)).isEmpty();
  }

  @Test
  @DisplayName("AC-002-08 an Owner of 25 Groups can create a 26th")
  void ac_002_08_no_limit_on_groups() {
    TestUser ana = user("Ana Novak", "ana@acme.com");
    for (int i = 1; i <= 25; i++) {
      group(ana, "Skupina " + i);
    }

    ana.post("/groups", Json.obj("name", "Skupina 26")).expect(201);

    assertThat(groupsOf(ana)).hasSize(26);
  }

  @Test
  @DisplayName("EX-002-01 a name of 100 characters is accepted")
  void ex_002_01_name_of_100_accepted() {
    TestUser ana = user("Ana Novak", "ana@acme.com");
    String name = chars(100, 'a');

    var created = ana.post("/groups", Json.obj("name", name)).expect(201).json();

    assertThat(created.path("name").asText()).isEqualTo(name);
  }

  @Test
  @DisplayName("EX-002-02 a name of 101 characters is refused")
  void ex_002_02_name_of_101_refused() {
    TestUser ana = user("Ana Novak", "ana@acme.com");

    ana.post("/groups", Json.obj("name", chars(101, 'a'))).expect(422);

    assertThat(groupsOf(ana)).isEmpty();
  }

  @Test
  @DisplayName("EX-002-03 a name of only spaces is refused")
  void ex_002_03_name_of_spaces_refused() {
    TestUser ana = user("Ana Novak", "ana@acme.com");

    ana.post("/groups", Json.obj("name", "     ")).expect(422);

    assertThat(groupsOf(ana)).isEmpty();
  }

  @Test
  @DisplayName("EX-002-04 the name 'Šola ČŽ' is stored unchanged")
  void ex_002_04_slovene_letters_unchanged() {
    TestUser ana = user("Ana Novak", "ana@acme.com");

    var created = ana.post("/groups", Json.obj("name", "Šola ČŽ")).expect(201).json();

    assertThat(created.path("name").asText()).isEqualTo("Šola ČŽ");
    assertThat(groupsOf(ana).get(0).path("name").asText()).isEqualTo("Šola ČŽ");
  }

  @Test
  @DisplayName("EX-002-05 'acme' is refused when Ana owns Acme")
  void ex_002_05_lowercase_duplicate_refused() {
    TestUser ana = user("Ana Novak", "ana@acme.com");
    group(ana, "Acme");

    ana.post("/groups", Json.obj("name", "acme")).expectError(409, "group.name_taken");
  }

  @Test
  @DisplayName("EX-002-06 a Group Acme in Trash does not block a new Acme")
  void ex_002_06_group_in_trash_does_not_count() {
    TestUser ana = user("Ana Novak", "ana@acme.com");
    Grp acme = group(ana, "Acme");
    ana.post(acme.toGroupTrash(), Json.obj("name", "Acme")).expect(204);

    ana.post("/groups", Json.obj("name", "Acme")).expect(201);
  }

  @Test
  @DisplayName("EX-002-07 an Editor of Boris's Acme may create her own Acme")
  void ex_002_07_uniqueness_is_per_owner() {
    TestUser ana = user("Ana Novak", "ana@acme.com");
    TestUser boris = user("Boris Kos", "boris@acme.com");
    Grp borisAcme = group(boris, "Acme");
    addMember(borisAcme, boris, ana, "EDITOR");

    ana.post("/groups", Json.obj("name", "Acme")).expect(201);

    assertThat(groupsOf(ana)).hasSize(2);
  }
}
