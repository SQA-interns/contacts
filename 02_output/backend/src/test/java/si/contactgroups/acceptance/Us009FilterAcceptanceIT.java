package si.contactgroups.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

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

/** US-009 Filter Contacts (query parameters of {@code GET /groups/{g}/contacts}). */
class Us009FilterAcceptanceIT extends AcceptanceTestBase {

  private TestUser ana;
  private Grp acme;

  @BeforeEach
  void anaOwnsAcme() {
    ana = user("Ana Novak", "ana@acme.com");
    acme = group(ana, "Acme");
  }

  private static String enc(String text) {
    return URLEncoder.encode(text, StandardCharsets.UTF_8);
  }

  private List<String> firstNames(String query) {
    return Json.field(contactPage(ana, acme, query).path("items"), "firstName");
  }

  private void named(String firstName, Object... more) {
    Object[] all = new Object[more.length + 2];
    all[0] = "firstName";
    all[1] = firstName;
    System.arraycopy(more, 0, all, 2, more.length);
    createContact(ana, acme, all);
  }

  private void abc() {
    named("A", "tags", Json.arr("a", "b"));
    named("B", "tags", Json.arr("a"));
    named("C", "tags", Json.arr("b"));
  }

  private void attributeContacts() {
    named("A", "attributes", attrs("kompetenca", "spletnoProgramiranje"));
    named("B", "attributes", attrs("kompetenca", "java"));
    named("C");
  }

  @Test
  @DisplayName("AC-009-01 'has all' of a, b gives only A")
  void ac_009_01_has_all() {
    abc();

    assertThat(firstNames("tags=a&tags=b&tagMode=all")).containsExactly("A");
  }

  @Test
  @DisplayName("AC-009-02 'has any' of a, b gives A, B and C")
  void ac_009_02_has_any() {
    abc();

    assertThat(firstNames("tags=a&tags=b&tagMode=any")).containsExactlyInAnyOrder("A", "B", "C");
  }

  @Test
  @DisplayName("AC-009-03 'has all' udelezenec2025 and Without-Tags neObvescaj gives only A")
  void ac_009_03_with_and_without_tags() {
    named("A", "tags", Json.arr("udelezenec2025"));
    named("B", "tags", Json.arr("udelezenec2025", "neObvescaj"));

    assertThat(firstNames("tags=udelezenec2025&tagMode=all&withoutTags=neObvescaj"))
        .containsExactly("A");
  }

  @Test
  @DisplayName("AC-009-04 the country filter SI gives only the SI Contacts")
  void ac_009_04_country() {
    named("Si", "country", "SI");
    named("At", "country", "AT");

    assertThat(firstNames("country=SI")).containsExactly("Si");
  }

  @Test
  @DisplayName("AC-009-05 the company filter Acme d.o.o. gives only the Acme Contacts")
  void ac_009_05_company() {
    named("A1", "company", "Acme d.o.o.");
    named("A2", "company", "Acme d.o.o.");
    named("B", "company", "Beta");

    assertThat(firstNames("company=" + enc("Acme d.o.o."))).containsExactlyInAnyOrder("A1", "A2");
  }

  @Test
  @DisplayName("AC-009-06 the Attribute filter kompetenca equals java gives only B")
  void ac_009_06_attribute_value() {
    attributeContacts();

    assertThat(firstNames("attrKey=kompetenca&attrValue=java")).containsExactly("B");
  }

  @Test
  @DisplayName("AC-009-07 the Attribute filter kompetenca exists gives A and B")
  void ac_009_07_attribute_exists() {
    attributeContacts();

    assertThat(firstNames("attrKey=kompetenca&attrExists=true"))
        .containsExactlyInAnyOrder("A", "B");
  }

  @Test
  @DisplayName("AC-009-08 a filter and the search text must both hold")
  void ac_009_08_filter_and_search_combined() {
    named("Ana", "lastName", "Novak", "country", "SI");
    named("Ana", "lastName", "Kos", "country", "AT");
    named("Boris", "lastName", "Novak", "country", "SI");

    var items = contactPage(ana, acme, "country=SI&q=ana").path("items");

    assertThat(Json.field(items, "lastName")).containsExactly("Novak");
    assertThat(Json.field(items, "firstName")).containsExactly("Ana");
  }

  @Test
  @DisplayName("AC-009-11 the Tag gost of Beta is not offered in Acme")
  void ac_009_11_only_values_of_the_group_offered() {
    named("A", "tags", Json.arr("udelezenec2025"), "company", "Acme d.o.o.", "country", "SI");
    Grp beta = group(ana, "Beta");
    createContact(
        ana,
        beta,
        "firstName",
        "X",
        "tags",
        Json.arr("gost"),
        "company",
        "Beta",
        "country",
        "AT",
        "attributes",
        attrs("projekt", "p"));

    var facets = ana.get(acme.facets()).expect(200).json();

    assertThat(Json.strings(facets.path("tags"))).containsExactly("udelezenec2025");
    assertThat(Json.strings(facets.path("companies"))).containsExactly("Acme d.o.o.");
    assertThat(Json.strings(facets.path("countries"))).containsExactly("SI");
    assertThat(Json.strings(facets.path("attributeKeys"))).isEmpty();
  }

  @Test
  @DisplayName("AC-009-12 a filter by a Tag x that no Contact has matches nothing")
  void ac_009_12_no_match() {
    abc();

    var page = contactPage(ana, acme, "tags=x&tagMode=all");

    assertThat(page.path("total").asInt()).isZero();
    assertThat(page.path("items")).isEmpty();
  }

  @Test
  @DisplayName("EX-009-01 no Tag chosen means no Tag filtering")
  void ex_009_01_no_tag_no_filter() {
    abc();

    assertThat(firstNames("tagMode=all")).hasSize(3);
  }

  @Test
  @DisplayName("EX-009-02 the same Tag in 'has all' and in Without-Tags matches nothing")
  void ex_009_02_contradiction_matches_nothing() {
    abc();

    assertThat(contactPage(ana, acme, "tags=a&tagMode=all&withoutTags=a").path("total").asInt())
        .isZero();
  }

  @Test
  @DisplayName("EX-009-03 a Contact without Attributes is not listed under 'exists'")
  void ex_009_03_exists_excludes_contacts_without_attributes() {
    attributeContacts();

    assertThat(firstNames("attrKey=kompetenca&attrExists=true")).doesNotContain("C");
  }

  @Test
  @DisplayName("EX-009-04 the value filter JAVA matches java")
  void ex_009_04_value_ignores_case() {
    attributeContacts();

    assertThat(firstNames("attrKey=kompetenca&attrValue=JAVA")).containsExactly("B");
  }

  @Test
  @DisplayName("EX-009-05 a Contact without country is not listed under a country filter")
  void ex_009_05_no_country_excluded() {
    named("Si", "country", "SI");
    named("Brez");

    assertThat(firstNames("country=SI")).containsExactly("Si");
  }

  @Test
  @DisplayName("EX-009-06 a Contact in Trash with the chosen Tag is not listed")
  void ex_009_06_trash_excluded() {
    named("A", "tags", Json.arr("gost"));
    String gone =
        createContact(ana, acme, "firstName", "B", "tags", Json.arr("gost")).path("id").asText();
    moveToTrash(ana, acme, gone);

    assertThat(firstNames("tags=gost&tagMode=all")).containsExactly("A");
  }

  @Test
  @DisplayName("EX-009-07 the company filter Čeh d.o.o. matches the stored Ceh d.o.o.")
  void ex_009_07_company_ignores_accents() {
    named("C", "company", "Ceh d.o.o.");
    named("B", "company", "Beta");

    assertThat(firstNames("company=" + enc("Čeh d.o.o."))).containsExactly("C");
  }
}
