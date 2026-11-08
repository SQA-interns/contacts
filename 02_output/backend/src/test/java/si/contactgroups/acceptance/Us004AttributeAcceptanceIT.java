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
import si.contactgroups.acceptance.support.Grp;
import si.contactgroups.acceptance.support.Json;
import si.contactgroups.acceptance.support.TestUser;

/** US-004 Add and change Attributes of a Contact (part of the Contact save). */
class Us004AttributeAcceptanceIT extends AcceptanceTestBase {

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

  private static List<JsonNode> numbered(int count) {
    List<JsonNode> pairs = new ArrayList<>();
    for (int i = 1; i <= count; i++) {
      pairs.add(Json.obj("key", "kljuc" + i, "value", "vrednost" + i));
    }
    return pairs;
  }

  @Test
  @DisplayName(
      "AC-004-01 adding kompetenca = spletnoProgramiranje stores it and raises the version by 1")
  void ac_004_01_add_attribute() {
    var saved =
        edit(ana, acme, anaNovak, "attributes", attrs("kompetenca", "spletnoProgramiranje"));

    assertThat(saved.path("version").asInt()).isEqualTo(2);
    assertThat(Json.field(read().path("attributes"), "key")).containsExactly("kompetenca");
    assertThat(Json.field(read().path("attributes"), "value"))
        .containsExactly("spletnoProgramiranje");
  }

  @Test
  @DisplayName("AC-004-02 a second pair with key Kompetenca is refused at the second pair")
  void ac_004_02_duplicate_key_ignoring_case() {
    edit(ana, acme, anaNovak, "attributes", attrs("kompetenca", "java"));

    var answer =
        save(ana, acme, read(), "attributes", attrs("kompetenca", "java", "Kompetenca", "sql"))
            .expect(422);

    assertThat(answer.errorCodes()).contains("validation.attribute_duplicate_key");
    assertThat(read().path("attributes")).hasSize(1);
    assertThat(read().path("version").asInt()).isEqualTo(2);
  }

  @Test
  @DisplayName("AC-004-03 a 51st Attribute is refused with the limit of 50")
  void ac_004_03_more_than_50_refused() {
    edit(ana, acme, anaNovak, "attributes", numbered(50));

    var answer = save(ana, acme, read(), "attributes", numbered(51)).expect(422);

    assertThat(answer.errorCodes()).contains("validation.attribute_limit");
    assertThat(read().path("attributes")).hasSize(50);
  }

  @Test
  @DisplayName(
      "AC-004-04 kompetenca is suggested from the Group, and the new key kompetentnost is accepted")
  void ac_004_04_keys_of_the_group_suggested() {
    createContact(ana, acme, "firstName", "Boris", "attributes", attrs("kompetenca", "java"));

    var facets = ana.get(acme.facets()).expect(200).json();
    assertThat(Json.strings(facets.path("attributeKeys"))).contains("kompetenca");

    edit(ana, acme, anaNovak, "attributes", attrs("kompetentnost", "visoka"));
    assertThat(Json.field(read().path("attributes"), "key")).containsExactly("kompetentnost");
  }

  @Test
  @DisplayName("AC-004-05 changing the value to java records key, old and new value in the History")
  void ac_004_05_change_recorded_in_history() {
    edit(ana, acme, anaNovak, "attributes", attrs("kompetenca", "spletnoProgramiranje"));

    edit(ana, acme, anaNovak, "attributes", attrs("kompetenca", "java"));

    JsonNode entry =
        history(ana, acme, anaNovak).stream()
            .filter(e -> e.path("action").asText().equals("ATTRIBUTE_CHANGED"))
            .findFirst()
            .orElseThrow();
    assertThat(entry.path("details").toString())
        .contains("kompetenca")
        .contains("spletnoProgramiranje")
        .contains("java");
  }

  @Test
  @DisplayName("AC-004-06 removing the pair removes it and records the removal")
  void ac_004_06_remove_recorded() {
    edit(ana, acme, anaNovak, "attributes", attrs("kompetenca", "java"));

    edit(ana, acme, anaNovak, "attributes", List.of());

    assertThat(read().path("attributes")).isEmpty();
    JsonNode entry =
        history(ana, acme, anaNovak).stream()
            .filter(e -> e.path("action").asText().equals("ATTRIBUTE_REMOVED"))
            .findFirst()
            .orElseThrow();
    assertThat(entry.path("details").toString()).contains("kompetenca");
  }

  @Test
  @DisplayName("AC-004-07 a Viewer sees the Attributes but a change request is refused")
  void ac_004_07_viewer_cannot_change() {
    edit(ana, acme, anaNovak, "attributes", attrs("kompetenca", "java"));
    TestUser viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");

    JsonNode seen = getContact(viola, acme, anaNovak);
    assertThat(Json.field(seen.path("attributes"), "value")).containsExactly("java");
    save(viola, acme, seen, "attributes", attrs("kompetenca", "sql"))
        .expectError(403, "auth.role_forbidden");

    assertThat(Json.field(read().path("attributes"), "value")).containsExactly("java");
    assertThat(read().path("version").asInt()).isEqualTo(2);
  }

  @Test
  @DisplayName("AC-004-08 the key projekt of another Group is not suggested in Acme")
  void ac_004_08_other_group_keys_not_suggested() {
    Grp beta = group(ana, "Beta");
    createContact(ana, beta, "firstName", "Cene", "attributes", attrs("projekt", "x"));

    var facets = ana.get(acme.facets()).expect(200).json();

    assertThat(Json.strings(facets.path("attributeKeys"))).doesNotContain("projekt");
  }

  @Test
  @DisplayName("AC-004-09 a new Attribute on a Contact Boris saved in between is refused")
  void ac_004_09_stale_version_refused() {
    TestUser boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
    JsonNode anaOpened = read();
    edit(boris, acme, anaNovak, "company", "Acme");

    save(ana, acme, anaOpened, "attributes", attrs("kompetenca", "java"))
        .expectError(409, "contact.changed_by_someone_else");

    assertThat(read().path("attributes")).isEmpty();
  }

  @ParameterizedTest(name = "EX-004-01 key of {0} characters: stored = {1}")
  @CsvSource({"50, true", "51, false"})
  void ex_004_01_key_length(int length, boolean stored) {
    var answer = save(ana, acme, read(), "attributes", attrs(chars(length, 'k'), "v"));

    assertThat(answer.status()).isEqualTo(stored ? 200 : 422);
    assertThat(read().path("attributes")).hasSize(stored ? 1 : 0);
  }

  @ParameterizedTest(name = "EX-004-02 value of {0} characters: stored = {1}")
  @CsvSource({"255, true", "256, false"})
  void ex_004_02_value_length(int length, boolean stored) {
    var answer = save(ana, acme, read(), "attributes", attrs("kljuc", chars(length, 'v')));

    assertThat(answer.status()).isEqualTo(stored ? 200 : 422);
    assertThat(read().path("attributes")).hasSize(stored ? 1 : 0);
  }

  @Test
  @DisplayName("EX-004-03 an empty key is refused")
  void ex_004_03_empty_key_refused() {
    save(ana, acme, read(), "attributes", attrs("", "vodja")).expect(422);

    assertThat(read().path("attributes")).isEmpty();
  }

  @Test
  @DisplayName("EX-004-04 an empty value is refused")
  void ex_004_04_empty_value_refused() {
    save(ana, acme, read(), "attributes", attrs("kompetenca", "")).expect(422);

    assertThat(read().path("attributes")).isEmpty();
  }

  @Test
  @DisplayName("EX-004-05 exactly 50 pairs are stored")
  void ex_004_05_exactly_50() {
    edit(ana, acme, anaNovak, "attributes", numbered(50));

    assertThat(read().path("attributes")).hasSize(50);
  }

  @Test
  @DisplayName("EX-004-06 the key 'delovno mesto' with a space is stored")
  void ex_004_06_key_with_space() {
    edit(ana, acme, anaNovak, "attributes", attrs("delovno mesto", "vodja"));

    assertThat(Json.field(read().path("attributes"), "key")).containsExactly("delovno mesto");
  }

  @Test
  @DisplayName("EX-004-07 kompetenca on Contact A and Kompetenca on Contact B are both stored")
  void ex_004_07_uniqueness_per_contact() {
    edit(ana, acme, anaNovak, "attributes", attrs("kompetenca", "java"));

    var other =
        createContact(ana, acme, "firstName", "Boris", "attributes", attrs("Kompetenca", "sql"));

    assertThat(Json.field(other.path("attributes"), "key")).containsExactly("Kompetenca");
    assertThat(Json.field(read().path("attributes"), "key")).containsExactly("kompetenca");
  }

  @Test
  @DisplayName("EX-004-08 the value <b>x</b> is stored as plain text")
  void ex_004_08_html_stored_as_text() {
    edit(ana, acme, anaNovak, "attributes", attrs("opis", "<b>x</b>"));

    assertThat(Json.field(read().path("attributes"), "value")).containsExactly("<b>x</b>");
  }

  @Test
  @DisplayName("EX-004-09 the value 42 is stored as the text 42")
  void ex_004_09_number_stored_as_text() {
    edit(ana, acme, anaNovak, "attributes", attrs("starost", "42"));

    JsonNode value = read().path("attributes").get(0).path("value");
    assertThat(value.isTextual()).isTrue();
    assertThat(value.asText()).isEqualTo("42");
  }
}
