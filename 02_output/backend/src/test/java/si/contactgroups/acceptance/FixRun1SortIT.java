package si.contactgroups.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.contactgroups.acceptance.support.AcceptanceTestBase;
import si.contactgroups.acceptance.support.Grp;
import si.contactgroups.acceptance.support.Json;
import si.contactgroups.acceptance.support.TestUser;
import si.contactgroups.domain.Normaliser;

/**
 * F-42, F-43 (fix loop 1): R-007-03 for every list column, not only the last name: texts compare
 * ignoring case and accents, the same in both directions.
 */
class FixRun1SortIT extends AcceptanceTestBase {

  private static final List<String> TEXTS =
      List.of(
          "ŠkofjaLoka", "sežana", "Šempeter", "Celje", "črnomelj", "ČRNUČE", "Bled", "ajdovščina");

  private TestUser ana;
  private Grp acme;

  @BeforeEach
  void anaOwnsAcme() {
    ana = user("Ana Novak", "ana@acme.com");
    acme = group(ana, "Acme");
  }

  private List<String> expected(boolean descending) {
    List<String> sorted = new ArrayList<>(TEXTS);
    Comparator<String> byNorm =
        Comparator.comparing(Normaliser::normalise, Comparator.naturalOrder());
    sorted.sort(descending ? byNorm.reversed() : byNorm);
    return sorted;
  }

  private List<String> column(String field, String sort, String dir) {
    List<JsonNode> items = contactItems(ana, acme, "sort=" + sort + "&dir=" + dir);
    return Json.field(Json.MAPPER.valueToTree(items), field);
  }

  @Test
  @DisplayName("R-007-03 the city column orders ignoring case and accents, both directions")
  void city_column() {
    TEXTS.forEach(t -> createContact(ana, acme, "lastName", "X", "city", t));
    assertThat(column("city", "city", "asc")).containsExactlyElementsOf(expected(false));
    assertThat(column("city", "city", "desc")).containsExactlyElementsOf(expected(true));
  }

  @Test
  @DisplayName("R-007-03 the company column orders ignoring case and accents, both directions")
  void company_column() {
    TEXTS.forEach(t -> createContact(ana, acme, "company", t));
    assertThat(column("company", "company", "asc")).containsExactlyElementsOf(expected(false));
    assertThat(column("company", "company", "desc")).containsExactlyElementsOf(expected(true));
  }

  @Test
  @DisplayName("R-007-03 the first name column orders ignoring case and accents, both directions")
  void first_name_column() {
    TEXTS.forEach(t -> createContact(ana, acme, "firstName", t));
    assertThat(column("firstName", "firstName", "asc")).containsExactlyElementsOf(expected(false));
    assertThat(column("firstName", "firstName", "desc")).containsExactlyElementsOf(expected(true));
  }

  @Test
  @DisplayName("R-007-03 the last name column orders ignoring case and accents, both directions")
  void last_name_column() {
    TEXTS.forEach(t -> createContact(ana, acme, "lastName", t));
    assertThat(column("lastName", "lastName", "asc")).containsExactlyElementsOf(expected(false));
    assertThat(column("lastName", "lastName", "desc")).containsExactlyElementsOf(expected(true));
  }

  @Test
  @DisplayName("R-007-03 the Tags column orders by the Tag names ignoring case and accents")
  void tags_column() {
    TEXTS.forEach(t -> createContact(ana, acme, "lastName", t, "tags", Json.arr(t)));
    List<String> asc = column("lastName", "tags", "asc");
    assertThat(asc).containsExactlyElementsOf(expected(false));
    assertThat(column("lastName", "tags", "desc")).containsExactlyElementsOf(expected(true));
  }
}
