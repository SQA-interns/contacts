package si.contactgroups.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** R-011-03, R-011-04, R-011-09: columns, cells and file name of the Export file. */
class ExportColumnsTest {

  private static ExportContact contact(
      ContactData data, List<String> tags, AttributePair... pairs) {
    return new ExportContact(data, tags, List.of(pairs));
  }

  private static ContactData named(String first, String last, String country) {
    return new ContactData(first, last, null, null, null, null, null, null, null, country, null);
  }

  @Test
  void theHeaderHasTheElevenFieldsAndTagsInTheLanguage() {
    assertThat(ExportColumns.header("en"))
        .containsExactly(
            "First name",
            "Last name",
            "E-mail",
            "Phone",
            "Company",
            "Job title",
            "Street and house number",
            "Postal code",
            "City",
            "Country",
            "Notes",
            "Tags");
    assertThat(ExportColumns.header("sl"))
        .containsExactly(
            "Ime",
            "Priimek",
            "E-pošta",
            "Telefon",
            "Podjetje",
            "Delovno mesto",
            "Ulica in hišna številka",
            "Poštna številka",
            "Kraj",
            "Država",
            "Opombe",
            "Značke");
  }

  @Test
  void attributeKeysAreOneColumnPerKeyIgnoringCaseSortedWithoutAccents() {
    var a = contact(named("A", "A", null), List.of(), new AttributePair("Šola", "x"));
    var b =
        contact(
            named("B", "B", null),
            List.of(),
            new AttributePair("kompetenca", "x"),
            new AttributePair("Kompetenca", "y"));
    var c = contact(named("C", "C", null), List.of(), new AttributePair("sola", "z"));

    assertThat(ExportColumns.attributeKeys(List.of(a, b, c)))
        .containsExactly("Kompetenca", "sola", "Šola");
  }

  @Test
  void noAttributesGiveNoAttributeColumns() {
    assertThat(ExportColumns.attributeKeys(List.of(contact(named("A", "A", null), List.of()))))
        .isEmpty();
  }

  @Test
  void aRowHoldsTheFieldsTheSortedTagsAndTheAttributeValuesMatchedIgnoringCase() {
    var c =
        contact(
            named("Ana", "Novak", "SI"),
            List.of("udelezenec2025", "neObvescaj"),
            new AttributePair("Kompetenca", "java"));

    assertThat(ExportColumns.row(c, List.of("kompetenca", "other"), "en"))
        .containsExactly(
            "Ana",
            "Novak",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "Slovenia",
            "",
            "neObvescaj, udelezenec2025",
            "java",
            "");
  }

  @Test
  void theCountryNameFollowsTheLanguageAndAnEmptyCountryStaysEmpty() {
    var si = contact(named("A", "B", "SI"), List.of());
    var none = contact(named("A", "B", null), List.of());

    assertThat(ExportColumns.row(si, List.of(), "sl").get(9)).isEqualTo("Slovenija");
    assertThat(ExportColumns.row(none, List.of(), "sl").get(9)).isEmpty();
  }

  @Test
  void theFileNameIsTheGroupNameAndTheDateWithForbiddenCharactersReplaced() {
    LocalDate day = LocalDate.of(2026, 11, 9);

    assertThat(ExportColumns.fileName("Acme", day)).isEqualTo("Acme_2026-11-09.xlsx");
    assertThat(ExportColumns.fileName("Acme/Beta: 2026", day))
        .isEqualTo("Acme_Beta_ 2026_2026-11-09.xlsx");
    assertThat(ExportColumns.fileName("a\\b*c?d\"e<f>g|h\u0001i", day))
        .isEqualTo("a_b_c_d_e_f_g_h_i_2026-11-09.xlsx");
  }
}
