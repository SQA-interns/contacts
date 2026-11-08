package si.contactgroups.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.contactgroups.acceptance.support.AcceptanceTestBase;
import si.contactgroups.acceptance.support.Grp;
import si.contactgroups.acceptance.support.Json;
import si.contactgroups.acceptance.support.TestUser;

/** US-008 Search Contacts (query parameter {@code q}). */
class Us008SearchAcceptanceIT extends AcceptanceTestBase {

  private TestUser ana;
  private Grp acme;

  @BeforeEach
  void anaOwnsAcme() {
    ana = user("Ana Novak", "ana@acme.com");
    acme = group(ana, "Acme");
  }

  private JsonNode search(String text) {
    return contactPage(ana, acme, "q=" + URLEncoder.encode(text, StandardCharsets.UTF_8));
  }

  private List<String> foundIds(String text) {
    return Json.field(search(text).path("items"), "id");
  }

  private String anaNovak(Object... more) {
    Object[] base = {"firstName", "Ana", "lastName", "Novak"};
    Object[] all = new Object[base.length + more.length];
    System.arraycopy(base, 0, all, 0, base.length);
    System.arraycopy(more, 0, all, base.length, more.length);
    return createContact(ana, acme, all).path("id").asText();
  }

  @Test
  @DisplayName("AC-008-01 Skerlj finds the last name Škerlj")
  void ac_008_01_accents_ignored() {
    String id = createContact(ana, acme, "lastName", "Škerlj").path("id").asText();
    createContact(ana, acme, "lastName", "Kos");

    assertThat(foundIds("Skerlj")).containsExactly(id);
  }

  @Test
  @DisplayName("AC-008-02 'ana nov' finds Ana Novak")
  void ac_008_02_words_inside_fields() {
    String id = anaNovak();
    createContact(ana, acme, "firstName", "Boris", "lastName", "Kos");

    assertThat(foundIds("ana nov")).containsExactly(id);
  }

  @Test
  @DisplayName("AC-008-03 'udelezenec' finds the Contact with the Tag udelezenec2025")
  void ac_008_03_tag_names_searched() {
    String id = anaNovak("tags", Json.arr("udelezenec2025"));
    createContact(ana, acme, "firstName", "Boris", "lastName", "Kos");

    assertThat(foundIds("udelezenec")).containsExactly(id);
  }

  @Test
  @DisplayName("AC-008-04 SPLETNO finds the Attribute value spletnoProgramiranje")
  void ac_008_04_attribute_values_searched() {
    String id = anaNovak("attributes", attrs("kompetenca", "spletnoProgramiranje"));
    createContact(ana, acme, "firstName", "Boris", "lastName", "Kos");

    assertThat(foundIds("SPLETNO")).containsExactly(id);
  }

  @Test
  @DisplayName("AC-008-05 the Attribute key kompetenca is not searched: no Contact matches")
  void ac_008_05_attribute_keys_not_searched() {
    anaNovak("attributes", attrs("kompetenca", "java"));

    JsonNode result = search("kompetenca");

    assertThat(result.path("total").asInt()).isZero();
    assertThat(result.path("items")).isEmpty();
  }

  @Test
  @DisplayName("AC-008-06 the phone 040 123 456 is not searched")
  void ac_008_06_phone_not_searched() {
    anaNovak("phone", "040 123 456");

    assertThat(search("040").path("total").asInt()).isZero();
  }

  @Test
  @DisplayName("AC-008-07 a Contact in Trash is not found")
  void ac_008_07_trash_not_searched() {
    moveToTrash(ana, acme, anaNovak());

    assertThat(search("Novak").path("total").asInt()).isZero();
  }

  @Test
  @DisplayName("AC-008-08 Ana Novak of Beta is not found in Acme")
  void ac_008_08_other_group_not_searched() {
    Grp beta = group(ana, "Beta");
    createContact(ana, beta, "firstName", "Ana", "lastName", "Novak");

    assertThat(search("Novak").path("total").asInt()).isZero();
  }

  @Test
  @DisplayName("AC-008-09 a search that matches 30 of 60 shows page 1 and 30 contacts")
  void ac_008_09_result_page_one_and_count() {
    for (int i = 1; i <= 60; i++) {
      createContact(ana, acme, "firstName", "Oseba" + i, "lastName", i % 2 == 0 ? "Sodo" : "Liho");
    }
    assertThat(contactPage(ana, acme, "page=3").path("items")).hasSize(10);

    JsonNode result = search("Sodo");

    assertThat(result.path("page").asInt()).isEqualTo(1);
    assertThat(result.path("total").asInt()).isEqualTo(30);
    assertThat(result.path("items")).hasSize(25);
  }

  @Test
  @DisplayName("EX-008-01 a search text of spaces gives the full list")
  void ex_008_01_spaces_are_no_search() {
    anaNovak();
    createContact(ana, acme, "firstName", "Boris");

    assertThat(search("   ").path("total").asInt()).isEqualTo(2);
  }

  @Test
  @DisplayName("EX-008-02 '%' matches no Contact when none contains %")
  void ex_008_02_percent_is_plain_text() {
    anaNovak();
    createContact(ana, acme, "firstName", "Boris");

    assertThat(search("%").path("total").asInt()).isZero();
  }

  @Test
  @DisplayName("EX-008-03 the text '; DROP TABLE contact; -- matches nothing and breaks nothing")
  void ex_008_03_injection_is_plain_text() {
    anaNovak();

    assertThat(search("'; DROP TABLE contact; --").path("total").asInt()).isZero();
    assertThat(contactPage(ana, acme, "").path("total").asInt()).isEqualTo(1);
  }

  @Test
  @DisplayName("EX-008-04 '<script>' matches nothing")
  void ex_008_04_script_is_plain_text() {
    anaNovak();

    assertThat(search("<script>").path("total").asInt()).isZero();
  }

  @Test
  @DisplayName("EX-008-05 ČEH finds Čeh and Ceh")
  void ex_008_05_accented_search_text() {
    String ceh1 = createContact(ana, acme, "lastName", "Čeh").path("id").asText();
    String ceh2 = createContact(ana, acme, "lastName", "Ceh").path("id").asText();
    createContact(ana, acme, "lastName", "Kos");

    assertThat(foundIds("ČEH")).containsExactlyInAnyOrder(ceh1, ceh2);
  }

  @Test
  @DisplayName("EX-008-06 'ana novak xyz' matches nothing: every word must match")
  void ex_008_06_all_words_must_match() {
    anaNovak();

    assertThat(search("ana novak xyz").path("total").asInt()).isZero();
  }

  @Test
  @DisplayName("EX-008-07 'novak ana' finds Ana Novak: word order does not matter")
  void ex_008_07_word_order_free() {
    String id = anaNovak();

    assertThat(foundIds("novak ana")).containsExactly(id);
  }
}
