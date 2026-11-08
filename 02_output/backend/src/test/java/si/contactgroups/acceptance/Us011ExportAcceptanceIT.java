package si.contactgroups.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.contactgroups.acceptance.support.AcceptanceTestBase;
import si.contactgroups.acceptance.support.Db;
import si.contactgroups.acceptance.support.Exports;
import si.contactgroups.acceptance.support.Grp;
import si.contactgroups.acceptance.support.Json;
import si.contactgroups.acceptance.support.Resp;
import si.contactgroups.acceptance.support.TestUser;
import si.contactgroups.acceptance.support.Xlsx;

/** US-011 Export the Selection to Excel, and EX-010-05 (a selected Contact deleted meanwhile). */
class Us011ExportAcceptanceIT extends AcceptanceTestBase {

  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private TestUser ana;
  private Grp acme;

  @BeforeEach
  void anaOwnsAcme() {
    ana = user("Ana Novak", "ana@acme.com");
    acme = group(ana, "Acme");
  }

  private String contact(Object... keyValues) {
    return createContact(ana, acme, keyValues).path("id").asText();
  }

  private Xlsx exportOk(TestUser user, Grp group, String lang, List<String> ids) {
    Resp answer = Exports.byIds(user, group, lang, ids).expect(200);
    assertThat(answer.header("Content-Type")).startsWith(XLSX);
    return Xlsx.read(answer.bytes());
  }

  private List<JsonNode> exportEntries(TestUser user, Grp group) {
    return activity(user, group, "action=CONTACTS_EXPORTED");
  }

  /** True if any number in the details of the entry (at any depth) equals {@code n}. */
  private static boolean detailsHoldNumber(JsonNode entry, int n) {
    return holds(entry.path("details"), n);
  }

  private static boolean holds(JsonNode node, int n) {
    if (node.isNumber()) {
      return node.asInt() == n;
    }
    if (node.isTextual()) {
      return node.asText().equals(String.valueOf(n));
    }
    for (JsonNode child : node) {
      if (holds(child, n)) {
        return true;
      }
    }
    return false;
  }

  @Test
  @DisplayName(
      "AC-011-02 3 selected Contacts give Acme_2026-11-08.xlsx with header, 3 rows, Tags and kompetenca")
  void ac_011_02_file_name_columns_and_rows() {
    clock.setNow(Instant.parse("2026-11-08T10:00:00Z"));
    List<String> ids =
        List.of(
            contact(
                "firstName",
                "Ana",
                "lastName",
                "Novak",
                "attributes",
                attrs("kompetenca", "spletnoProgramiranje")),
            contact("firstName", "Boris", "lastName", "Kos"),
            contact("firstName", "Cene", "lastName", "Zupan"));

    Resp answer = Exports.byIds(ana, acme, "en", ids).expect(200);
    Xlsx file = Xlsx.read(answer.bytes());

    assertThat(Exports.fileName(answer)).isEqualTo("Acme_2026-11-08.xlsx");
    assertThat(file.sheetCount()).isEqualTo(1);
    List<String> expected = new ArrayList<>(Exports.HEADER_EN);
    expected.add("kompetenca");
    assertThat(file.header()).containsExactlyElementsOf(expected);
    assertThat(file.rows()).hasSize(3);
    assertThat(file.column("kompetenca")).containsExactlyInAnyOrder("spletnoProgramiranje", "", "");
    assertThat(file.column("Last name")).containsExactlyInAnyOrder("Novak", "Kos", "Zupan");
  }

  @Test
  @DisplayName("AC-011-03 the Tags cell reads 'neObvescaj, udelezenec2025'")
  void ac_011_03_tags_cell_sorted() {
    String id = contact("firstName", "Ana", "tags", Json.arr("udelezenec2025", "neObvescaj"));

    Xlsx file = exportOk(ana, acme, "en", List.of(id));

    assertThat(file.column("Tags")).containsExactly("neObvescaj, udelezenec2025");
  }

  @Test
  @DisplayName("AC-011-04 =HYPERLINK(\"http://x\") and -1+1 are written as text, never as formulas")
  void ac_011_04_formula_like_values_are_text() {
    String id = contact("lastName", "=HYPERLINK(\"http://x\")", "notes", "-1+1");

    Xlsx file = exportOk(ana, acme, "en", List.of(id));

    assertThat(file.nonText()).isEmpty();
    assertThat(file.column("Last name")).containsExactly("=HYPERLINK(\"http://x\")");
    assertThat(file.column("Notes")).containsExactly("-1+1");
  }

  @Test
  @DisplayName("AC-011-05 a Selection of 10,001 Contacts gives no file and no History entry")
  void ac_011_05_more_than_10000_refused() {
    Db.insertContacts(acme.id(), ana.userId(), 10_001);

    Resp answer = Exports.byFilter(ana, acme, "en", Json.obj()).expectError(422, "export.too_many");

    assertThat(answer.header("Content-Type")).doesNotStartWith(XLSX);
    assertThat(exportEntries(ana, acme)).isEmpty();
  }

  @Test
  @DisplayName("AC-011-06 a Viewer's export call is refused; no file, no History entry")
  void ac_011_06_viewer_refused() {
    String id = contact("firstName", "Ana");
    TestUser viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");

    Resp answer =
        Exports.byIds(viola, acme, "en", List.of(id)).expectError(403, "auth.role_forbidden");

    assertThat(answer.header("Content-Type")).doesNotStartWith(XLSX);
    assertThat(exportEntries(ana, acme)).isEmpty();
  }

  @Test
  @DisplayName("AC-011-07 the export of 3 Contacts by Boris is listed with Boris, the time and 3")
  void ac_011_07_export_recorded() {
    TestUser boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
    List<String> ids =
        List.of(contact("firstName", "A"), contact("firstName", "B"), contact("firstName", "C"));
    clock.setNow(Instant.parse("2026-11-08T10:00:00Z"));

    exportOk(boris, acme, "en", ids);

    List<JsonNode> entries = exportEntries(ana, acme);
    assertThat(entries).hasSize(1);
    JsonNode entry = entries.get(0);
    assertThat(entry.path("userName").asText()).isEqualTo("Boris Kos");
    assertThat(Instant.parse(entry.path("occurredAt").asText()))
        .isBetween(Instant.parse("2026-11-08T10:00:00Z"), Instant.parse("2026-11-08T10:05:00Z"));
    assertThat(detailsHoldNumber(entry, 3)).as("details %s hold 3", entry.path("details")).isTrue();
  }

  @Test
  @DisplayName(
      "AC-011-08 'select all N results' of udelezenec2025 without neObvescaj exports exactly those")
  void ac_011_08_filter_selection_exports_matching() {
    contact("lastName", "Ena", "tags", Json.arr("udelezenec2025"));
    contact("lastName", "Dva", "tags", Json.arr("udelezenec2025"));
    contact("lastName", "Tri", "tags", Json.arr("udelezenec2025", "neObvescaj"));
    contact("lastName", "Stiri");

    Resp answer =
        Exports.byFilter(
                ana,
                acme,
                "en",
                Json.obj(
                    "tags",
                    Json.arr("udelezenec2025"),
                    "tagMode",
                    "all",
                    "withoutTags",
                    Json.arr("neObvescaj")))
            .expect(200);
    Xlsx file = Xlsx.read(answer.bytes());

    assertThat(file.column("Last name")).containsExactlyInAnyOrder("Ena", "Dva");
    assertThat(file.column("Tags")).noneMatch(tags -> tags.contains("neObvescaj"));
  }

  @Test
  @DisplayName("AC-011-09 a selected Contact deleted before the export is left out and not counted")
  void ac_011_09_deleted_contact_left_out() {
    List<String> ids =
        List.of(contact("lastName", "Ena"), contact("lastName", "Dva"), contact("lastName", "Tri"));
    moveToTrash(ana, acme, ids.get(2));

    Xlsx file = exportOk(ana, acme, "en", ids);

    assertThat(file.column("Last name")).containsExactlyInAnyOrder("Ena", "Dva");
    assertThat(detailsHoldNumber(exportEntries(ana, acme).get(0), 2)).isTrue();
  }

  @Test
  @DisplayName("EX-010-05 a selected Contact deleted by another Member is not exported")
  void ex_010_05_selection_without_deleted_contact() {
    TestUser boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
    List<String> ids = List.of(contact("lastName", "Ena"), contact("lastName", "Dva"));
    moveToTrash(boris, acme, ids.get(1));

    Xlsx file = exportOk(ana, acme, "en", ids);

    assertThat(file.rows()).hasSize(1);
    assertThat(file.column("Last name")).containsExactly("Ena");
  }

  @Test
  @DisplayName("AC-011-10 header cells are English with lang en and Slovene with lang sl")
  void ac_011_10_header_language() {
    String id = contact("firstName", "Ana", "country", "SI");

    Xlsx en = exportOk(ana, acme, "en", List.of(id));
    Xlsx sl = exportOk(ana, acme, "sl", List.of(id));

    assertThat(en.header()).containsExactlyElementsOf(Exports.HEADER_EN);
    assertThat(sl.header()).containsExactlyElementsOf(Exports.HEADER_SL);
  }

  @Test
  @DisplayName("AC-011-11 the row shows company Beta that Boris saved after the selection")
  void ac_011_11_data_at_export_time() {
    TestUser boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
    String id = contact("firstName", "Ana", "company", "Acme");
    edit(boris, acme, id, "company", "Beta");

    Xlsx file = exportOk(ana, acme, "en", List.of(id));

    assertThat(file.column("Company")).containsExactly("Beta");
  }

  @Test
  @DisplayName("EX-011-01 exactly 10,000 Contacts give a file with 10,000 rows")
  void ex_011_01_exactly_10000() {
    Db.insertContacts(acme.id(), ana.userId(), 10_000);

    Resp answer = Exports.byFilter(ana, acme, "en", Json.obj()).expect(200);

    assertThat(Xlsx.read(answer.bytes()).rows()).hasSize(10_000);
  }

  @Test
  @DisplayName("EX-011-02 10,001 Contacts selected by id are refused")
  void ex_011_02_10001_by_ids_refused() {
    Db.insertContacts(acme.id(), ana.userId(), 10_001);
    List<String> ids = allContactIds(ana, acme, "");
    assertThat(ids).hasSize(10_001);

    Exports.byIds(ana, acme, "en", ids).expectError(422, "export.too_many");

    assertThat(exportEntries(ana, acme)).isEmpty();
  }

  @Test
  @DisplayName("EX-011-03 values starting with =, +, -, @ and the phone +386 40 123 456 are text")
  void ex_011_03_formula_prefixes_are_text() {
    String id =
        contact(
            "firstName", "=1+2",
            "lastName", "@Novak",
            "company", "-Acme",
            "jobTitle", "+Vodja",
            "phone", "+386 40 123 456");

    Xlsx file = exportOk(ana, acme, "en", List.of(id));

    assertThat(file.nonText()).isEmpty();
    List<String> row = file.rows().get(0);
    assertThat(file.cell(row, "First name")).isEqualTo("=1+2");
    assertThat(file.cell(row, "Last name")).isEqualTo("@Novak");
    assertThat(file.cell(row, "Company")).isEqualTo("-Acme");
    assertThat(file.cell(row, "Job title")).isEqualTo("+Vodja");
    assertThat(file.cell(row, "Phone")).isEqualTo("+386 40 123 456");
  }

  @Test
  @DisplayName("EX-011-04 without Attributes in the Selection there are no Attribute columns")
  void ex_011_04_no_attribute_columns() {
    String id = contact("firstName", "Ana");

    assertThat(exportOk(ana, acme, "en", List.of(id)).header())
        .containsExactlyElementsOf(Exports.HEADER_EN);
  }

  @Test
  @DisplayName("EX-011-05 the keys Kompetenca and kompetenca make one column")
  void ex_011_05_keys_differing_in_case_one_column() {
    String a = contact("lastName", "A", "attributes", attrs("Kompetenca", "java"));
    String b = contact("lastName", "B", "attributes", attrs("kompetenca", "sql"));

    Xlsx file = exportOk(ana, acme, "en", List.of(a, b));

    List<String> extra = file.header().subList(Exports.HEADER_EN.size(), file.header().size());
    assertThat(extra).hasSize(1);
    assertThat(extra.get(0)).isEqualToIgnoringCase("kompetenca");
    assertThat(file.column(extra.get(0))).containsExactlyInAnyOrder("java", "sql");
  }

  @Test
  @DisplayName("EX-011-06 the Group name Acme/Beta: 2026 gives a file name with _ for / and :")
  void ex_011_06_file_name_characters_replaced() {
    clock.setNow(Instant.parse("2026-11-08T10:00:00Z"));
    Grp odd = group(ana, "Acme/Beta: 2026");
    String id = createContact(ana, odd, "firstName", "Ana").path("id").asText();

    Resp answer = Exports.byIds(ana, odd, "en", List.of(id)).expect(200);

    assertThat(Exports.fileName(answer)).isEqualTo("Acme_Beta_ 2026_2026-11-08.xlsx");
  }

  @Test
  @DisplayName("EX-011-07 the names Škerlj, Čeh, Žagar are written unchanged")
  void ex_011_07_slovene_letters() {
    String id = contact("firstName", "Čeh", "lastName", "Škerlj", "company", "Žagar");

    List<String> row = exportOk(ana, acme, "sl", List.of(id)).rows().get(0);

    assertThat(row).contains("Čeh", "Škerlj", "Žagar");
  }

  @Test
  @DisplayName("EX-011-08 a Contact without Tags has an empty Tags cell")
  void ex_011_08_empty_tags_cell() {
    String id = contact("firstName", "Ana");

    assertThat(exportOk(ana, acme, "en", List.of(id)).column("Tags")).containsExactly("");
  }

  @Test
  @DisplayName("EX-011-09 notes of 5,000 characters are written in full")
  void ex_011_09_long_notes_in_full() {
    String notes = chars(5000, 'n');
    String id = contact("firstName", "Ana", "notes", notes);

    assertThat(exportOk(ana, acme, "en", List.of(id)).column("Notes")).containsExactly(notes);
  }

  @Test
  @DisplayName(
      "EX-011-10 an export at 23:30 UTC on 2026-11-08 is dated 2026-11-09 (Europe/Ljubljana)")
  void ex_011_10_date_in_ljubljana() {
    String id = contact("firstName", "Ana");
    clock.setNow(Instant.parse("2026-11-08T23:30:00Z"));

    Resp answer = Exports.byIds(ana, acme, "en", List.of(id)).expect(200);

    assertThat(Exports.fileName(answer)).isEqualTo("Acme_2026-11-09.xlsx");
  }
}
