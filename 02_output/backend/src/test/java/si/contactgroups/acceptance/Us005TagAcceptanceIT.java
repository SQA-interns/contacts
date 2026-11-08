package si.contactgroups.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import si.contactgroups.acceptance.support.AcceptanceTestBase;
import si.contactgroups.acceptance.support.Grp;
import si.contactgroups.acceptance.support.Json;
import si.contactgroups.acceptance.support.TestUser;

/** US-005 Tag a Contact (part of the Contact save). */
class Us005TagAcceptanceIT extends AcceptanceTestBase {

  private TestUser ana;
  private Grp acme;
  private String anaNovak;

  @BeforeEach
  void anaNovakInAcme() {
    ana = user("Ana Novak", "ana@acme.com");
    acme = group(ana, "Acme");
    anaNovak =
        createContact(ana, acme, "firstName", "Ana", "lastName", "Novak").path("id").asText();
  }

  private JsonNode read() {
    return getContact(ana, acme, anaNovak);
  }

  private List<String> tagsOf(String contactId) {
    return Json.strings(getContact(ana, acme, contactId).path("tags"));
  }

  private List<String> groupTags() {
    return tagList(ana, acme).stream().map(t -> t.path("name").asText()).toList();
  }

  private static List<String> numbered(int count) {
    List<String> tags = new ArrayList<>();
    for (int i = 1; i <= count; i++) {
      tags.add("znacka" + i);
    }
    return tags;
  }

  @Test
  @DisplayName(
      "AC-005-01 typing udelezenec2025 creates the Tag in Acme, puts it on the Contact, version +1")
  void ac_005_01_new_tag_created_and_put_on() {
    var saved = edit(ana, acme, anaNovak, "tags", Json.arr("udelezenec2025"));

    assertThat(saved.path("version").asInt()).isEqualTo(2);
    assertThat(tagsOf(anaNovak)).containsExactly("udelezenec2025");
    assertThat(groupTags()).containsExactly("udelezenec2025");
  }

  @Test
  @DisplayName("AC-005-02 UDELEZENEC2025 reuses the existing Tag udelezenec2025")
  void ac_005_02_existing_tag_reused_ignoring_case() {
    edit(ana, acme, anaNovak, "tags", Json.arr("udelezenec2025"));
    String borisKos =
        createContact(ana, acme, "firstName", "Boris", "lastName", "Kos").path("id").asText();

    edit(ana, acme, borisKos, "tags", Json.arr("UDELEZENEC2025"));

    assertThat(tagsOf(borisKos)).containsExactly("udelezenec2025");
    assertThat(groupTags()).containsExactly("udelezenec2025");
  }

  @Test
  @DisplayName("AC-005-03 'udelezenec 2025' with a space is refused")
  void ac_005_03_tag_with_space_refused() {
    var answer = save(ana, acme, read(), "tags", Json.arr("udelezenec 2025")).expect(422);

    assertThat(answer.errorCodes()).contains("validation.tag_invalid");
    assertThat(tagsOf(anaNovak)).isEmpty();
    assertThat(groupTags()).isEmpty();
  }

  @Test
  @DisplayName("AC-005-04 a 51st Tag is refused with the limit")
  void ac_005_04_more_than_50_refused() {
    edit(ana, acme, anaNovak, "tags", numbered(50));

    var answer = save(ana, acme, read(), "tags", numbered(51)).expect(422);

    assertThat(answer.errorCodes()).contains("validation.tag_limit");
    assertThat(tagsOf(anaNovak)).hasSize(50);
  }

  @Test
  @DisplayName("AC-005-05 removing the Tag removes only the link and records it in the History")
  void ac_005_05_remove_tag_keeps_group_tag() {
    edit(ana, acme, anaNovak, "tags", Json.arr("udelezenec2025"));

    edit(ana, acme, anaNovak, "tags", Json.arr());

    assertThat(tagsOf(anaNovak)).isEmpty();
    assertThat(groupTags()).containsExactly("udelezenec2025");
    assertThat(actions(history(ana, acme, anaNovak))).contains("TAG_ADDED", "TAG_REMOVED");
  }

  @Test
  @DisplayName("AC-005-06 the Tag gost of Beta is not suggested in Acme")
  void ac_005_06_other_group_tag_not_suggested() {
    Grp beta = group(ana, "Beta");
    createContact(ana, beta, "firstName", "Cene", "tags", Json.arr("gost"));

    var facets = ana.get(acme.facets()).expect(200).json();

    assertThat(Json.strings(facets.path("tags"))).doesNotContain("gost");
    assertThat(groupTags()).doesNotContain("gost");
  }

  @Test
  @DisplayName(
      "AC-005-07 the Tags neObvescaj and udelezenec2025 are shown in the list and on the Contact")
  void ac_005_07_all_tags_shown() {
    edit(ana, acme, anaNovak, "tags", Json.arr("neObvescaj", "udelezenec2025"));
    TestUser viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");

    JsonNode item = contactItems(viola, acme, "").get(0);
    assertThat(Json.strings(item.path("tags")))
        .containsExactlyInAnyOrder("neObvescaj", "udelezenec2025");
    assertThat(Json.strings(getContact(viola, acme, anaNovak).path("tags")))
        .containsExactlyInAnyOrder("neObvescaj", "udelezenec2025");
  }

  @Test
  @DisplayName("AC-005-08 a Viewer cannot add a Tag")
  void ac_005_08_viewer_cannot_tag() {
    TestUser viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");

    save(viola, acme, getContact(viola, acme, anaNovak), "tags", Json.arr("gost"))
        .expectError(403, "auth.role_forbidden");

    assertThat(tagsOf(anaNovak)).isEmpty();
    assertThat(groupTags()).isEmpty();
    assertThat(read().path("version").asInt()).isEqualTo(1);
  }

  @Test
  @DisplayName("EX-005-01 Tag names of 1 and 50 characters are accepted")
  void ex_005_01_lengths_1_and_50_accepted() {
    edit(ana, acme, anaNovak, "tags", Json.arr("a", chars(50, 'b')));

    assertThat(tagsOf(anaNovak)).containsExactlyInAnyOrder("a", chars(50, 'b'));
  }

  @Test
  @DisplayName("EX-005-01 a Tag name of 51 characters is refused")
  void ex_005_01_length_51_refused() {
    save(ana, acme, read(), "tags", Json.arr(chars(51, 'b'))).expect(422);

    assertThat(tagsOf(anaNovak)).isEmpty();
  }

  @Test
  @DisplayName("EX-005-02 an empty Tag name is refused")
  void ex_005_02_empty_refused() {
    save(ana, acme, read(), "tags", Json.arr("")).expect(422);

    assertThat(tagsOf(anaNovak)).isEmpty();
  }

  @Test
  @DisplayName("EX-005-03 the Tag name č_š-ž1 is accepted")
  void ex_005_03_letters_digits_dash_underscore() {
    edit(ana, acme, anaNovak, "tags", Json.arr("č_š-ž1"));

    assertThat(tagsOf(anaNovak)).containsExactly("č_š-ž1");
  }

  @ParameterizedTest(name = "EX-005-04 the Tag name {0} is refused")
  @ValueSource(strings = {"a/b", "a.b", "a@b"})
  void ex_005_04_other_characters_refused(String name) {
    save(ana, acme, read(), "tags", Json.arr(name)).expect(422);

    assertThat(tagsOf(anaNovak)).isEmpty();
    assertThat(groupTags()).isEmpty();
  }

  @Test
  @DisplayName("EX-005-05 Neobvescaj uses the existing Tag neObvescaj")
  void ex_005_05_existing_spelling_used() {
    createContact(ana, acme, "firstName", "Boris", "tags", Json.arr("neObvescaj"));

    edit(ana, acme, anaNovak, "tags", Json.arr("Neobvescaj"));

    assertThat(tagsOf(anaNovak)).containsExactly("neObvescaj");
    assertThat(groupTags()).containsExactly("neObvescaj");
  }

  @Test
  @DisplayName("EX-005-06 the same Tag typed twice is put on the Contact once")
  void ex_005_06_same_tag_twice_once() {
    edit(ana, acme, anaNovak, "tags", Json.arr("gost", "gost"));

    assertThat(tagsOf(anaNovak)).containsExactly("gost");
  }

  @Test
  @DisplayName("EX-005-07 exactly 50 Tags are accepted")
  void ex_005_07_exactly_50() {
    edit(ana, acme, anaNovak, "tags", numbered(50));

    assertThat(tagsOf(anaNovak)).hasSize(50);
  }

  @Test
  @DisplayName("EX-005-08 adding a Tag with a stale version is refused")
  void ex_005_08_stale_version_refused() {
    JsonNode opened = read();
    edit(ana, acme, anaNovak, "company", "Acme");

    save(ana, acme, opened, "tags", Json.arr("gost"))
        .expectError(409, "contact.changed_by_someone_else");

    assertThat(tagsOf(anaNovak)).isEmpty();
  }
}
