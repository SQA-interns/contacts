package si.contactgroups.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.contactgroups.acceptance.support.AcceptanceTestBase;
import si.contactgroups.acceptance.support.Grp;
import si.contactgroups.acceptance.support.Json;
import si.contactgroups.acceptance.support.TestUser;

/** US-007 Browse the contact list ({@code GET /groups/{g}/contacts}, 25 per page). */
class Us007ContactListAcceptanceIT extends AcceptanceTestBase {

  private TestUser ana;
  private Grp acme;

  @BeforeEach
  void anaOwnsAcme() {
    ana = user("Ana Novak", "ana@acme.com");
    acme = group(ana, "Acme");
  }

  /** Creates Contacts with last names Priimek01..PriimekNN in a shuffled order. */
  private void numbered(int count) {
    List<Integer> order = new ArrayList<>();
    for (int i = 1; i <= count; i++) {
      order.add(i);
    }
    Collections.shuffle(order, new Random(7));
    for (int i : order) {
      createContact(ana, acme, "firstName", "Ime", "lastName", String.format("Priimek%02d", i));
    }
  }

  private List<String> lastNames(String query) {
    return Json.field(Json.MAPPER.valueToTree(contactItems(ana, acme, query)), "lastName");
  }

  private static List<String> range(int from, int to) {
    List<String> out = new ArrayList<>();
    for (int i = from; i <= to; i++) {
      out.add(String.format("Priimek%02d", i));
    }
    return out;
  }

  @Test
  @DisplayName("AC-007-01 page 1 of 60 Contacts shows 25 sorted by last name and the total 60")
  void ac_007_01_first_page_sorted_with_total() {
    numbered(60);

    JsonNode page = contactPage(ana, acme, "");

    assertThat(page.path("total").asInt()).isEqualTo(60);
    assertThat(page.path("pageSize").asInt()).isEqualTo(25);
    assertThat(page.path("page").asInt()).isEqualTo(1);
    assertThat(Json.field(page.path("items"), "lastName")).containsExactlyElementsOf(range(1, 25));
  }

  @Test
  @DisplayName("AC-007-02 page 3 of 60 Contacts shows 10")
  void ac_007_02_third_page_has_ten() {
    numbered(60);

    assertThat(lastNames("page=3")).containsExactlyElementsOf(range(51, 60));
  }

  @Test
  @DisplayName("AC-007-03 sorting by company twice orders ascending, then descending")
  void ac_007_03_sort_by_company_both_directions() {
    createContact(ana, acme, "firstName", "B", "company", "Beta");
    createContact(ana, acme, "firstName", "C", "company", "Cezar");
    createContact(ana, acme, "firstName", "A", "company", "Acme");

    List<JsonNode> asc = contactItems(ana, acme, "sort=company&dir=asc");
    List<JsonNode> desc = contactItems(ana, acme, "sort=company&dir=desc");

    assertThat(Json.field(Json.MAPPER.valueToTree(asc), "company"))
        .containsExactly("Acme", "Beta", "Cezar");
    assertThat(Json.field(Json.MAPPER.valueToTree(desc), "company"))
        .containsExactly("Cezar", "Beta", "Acme");
  }

  @Test
  @DisplayName("AC-007-06 a Contact in Trash is neither listed nor counted")
  void ac_007_06_trash_not_listed() {
    createContact(ana, acme, "lastName", "Novak");
    String gone = createContact(ana, acme, "lastName", "Kos").path("id").asText();
    moveToTrash(ana, acme, gone);

    JsonNode page = contactPage(ana, acme, "");

    assertThat(page.path("total").asInt()).isEqualTo(1);
    assertThat(Json.field(page.path("items"), "id")).doesNotContain(gone);
  }

  @Test
  @DisplayName(
      "AC-007-08 a Member of Acme only gets not found and no data for the Contacts of Beta")
  void ac_007_08_other_group_not_found() {
    TestUser boris = user("Boris Kos", "boris@acme.com");
    Grp beta = group(boris, "Beta");
    createContact(boris, beta, "lastName", "Betovec");
    TestUser cene = member(acme, ana, "Cene Zupan", "cene@acme.com", "EDITOR");

    var answer = cene.get(beta.contacts()).expect(404);

    assertThat(answer.body()).doesNotContain("Betovec");
  }

  @Test
  @DisplayName("EX-007-01 exactly 25 Contacts fit on one page")
  void ex_007_01_exactly_25_one_page() {
    numbered(25);

    assertThat(lastNames("page=1")).hasSize(25);
    assertThat(lastNames("page=2")).isEmpty();
  }

  @Test
  @DisplayName("EX-007-02 26 Contacts make two pages of 25 and 1")
  void ex_007_02_26_two_pages() {
    numbered(26);

    assertThat(lastNames("page=1")).hasSize(25);
    assertThat(lastNames("page=2")).containsExactly("Priimek26");
  }

  @Test
  @DisplayName("EX-007-03 a Contact without a last name is listed after those with one")
  void ex_007_03_without_last_name_last() {
    createContact(ana, acme, "firstName", "Brez", "company", "Acme");
    createContact(ana, acme, "lastName", "Zupan");
    createContact(ana, acme, "lastName", "Ahac");

    List<JsonNode> items = contactItems(ana, acme, "");

    assertThat(items.get(0).path("lastName").asText()).isEqualTo("Ahac");
    assertThat(items.get(1).path("lastName").asText()).isEqualTo("Zupan");
    assertThat(items.get(2).path("firstName").asText()).isEqualTo("Brez");
  }

  @Test
  @DisplayName("EX-007-04 equal last names Novak are ordered by first name: Ana before Boris")
  void ex_007_04_ties_by_first_name() {
    createContact(ana, acme, "firstName", "Boris", "lastName", "Novak");
    createContact(ana, acme, "firstName", "Ana", "lastName", "Novak");

    List<JsonNode> items = contactItems(ana, acme, "");

    assertThat(Json.field(Json.MAPPER.valueToTree(items), "firstName"))
        .containsExactly("Ana", "Boris");
  }

  @Test
  @DisplayName("EX-007-05 Čeh, Cvetko, Cerar are ordered ignoring accents: Cerar, Čeh, Cvetko")
  void ex_007_05_sort_ignores_accents() {
    createContact(ana, acme, "lastName", "Čeh");
    createContact(ana, acme, "lastName", "Cvetko");
    createContact(ana, acme, "lastName", "Cerar");

    assertThat(lastNames("")).containsExactly("Cerar", "Čeh", "Cvetko");
    assertThat(lastNames("sort=lastName&dir=asc")).containsExactly("Cerar", "Čeh", "Cvetko");
  }

  @Test
  @DisplayName("EX-007-06 page 99 of a 60-Contact list is empty with the total 60")
  void ex_007_06_page_beyond_end() {
    numbered(60);

    JsonNode page = contactPage(ana, acme, "page=99");

    assertThat(page.path("items")).isEmpty();
    assertThat(page.path("total").asInt()).isEqualTo(60);
  }
}
