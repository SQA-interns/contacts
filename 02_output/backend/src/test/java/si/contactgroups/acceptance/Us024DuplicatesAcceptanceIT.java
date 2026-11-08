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
import si.contactgroups.acceptance.support.TestUser;

/** US-024 Find duplicate Contacts ({@code GET /groups/{g}/duplicates}, read only). */
class Us024DuplicatesAcceptanceIT extends AcceptanceTestBase {

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

  private List<JsonNode> sets() {
    return Json.list(ana.get(acme.duplicates()).expect(200).json());
  }

  private static List<String> ids(JsonNode set) {
    return Json.field(set.path("contacts"), "id");
  }

  @Test
  @DisplayName(
      "AC-024-01 three e-mails equal ignoring case give one certain set of 3 sharing ana@acme.com")
  void ac_024_01_same_email_certain() {
    String a = contact("firstName", "Ana", "email", "ana@acme.com");
    String b = contact("firstName", "Anica", "email", "ANA@acme.com");
    String c = contact("firstName", "Anja", "email", "Ana@Acme.com");

    List<JsonNode> sets = sets();

    assertThat(sets).hasSize(1);
    JsonNode set = sets.get(0);
    assertThat(set.path("mark").asText()).isEqualTo("certain");
    assertThat(ids(set)).containsExactlyInAnyOrder(a, b, c);
    assertThat(set.path("reasonCode").asText()).isNotBlank();
    assertThat(set.path("reasonParams").toString()).contains("ana@acme.com");
  }

  @Test
  @DisplayName("AC-024-02 Ana Novak/Acme and ana novak/ACME give one probable set of 2")
  void ac_024_02_same_name_and_company_probable() {
    String a = contact("firstName", "Ana", "lastName", "Novak", "company", "Acme");
    String b =
        contact("firstName", "ana", "lastName", "novak", "company", "ACME", "email", "a@x.si");

    List<JsonNode> sets = sets();

    assertThat(sets).hasSize(1);
    assertThat(sets.get(0).path("mark").asText()).isEqualTo("probable");
    assertThat(ids(sets.get(0))).containsExactlyInAnyOrder(a, b);
  }

  @Test
  @DisplayName(
      "AC-024-03 the same name and the phones 040 123 456 / 040-123-456 give one probable set")
  void ac_024_03_same_name_and_phone_probable() {
    String a =
        contact("firstName", "Ana", "lastName", "Novak", "phone", "040 123 456", "company", "Acme");
    String b =
        contact("firstName", "Ana", "lastName", "Novak", "phone", "040-123-456", "company", "Beta");

    List<JsonNode> sets = sets();

    assertThat(sets).hasSize(1);
    assertThat(sets.get(0).path("mark").asText()).isEqualTo("probable");
    assertThat(ids(sets.get(0))).containsExactlyInAnyOrder(a, b);
  }

  @Test
  @DisplayName("AC-024-04 the same name with different company and phone gives no set")
  void ac_024_04_same_name_only_no_set() {
    contact("firstName", "Ana", "lastName", "Novak", "phone", "040 111 111", "company", "Acme");
    contact("firstName", "Ana", "lastName", "Novak", "phone", "040 222 222", "company", "Beta");

    assertThat(sets()).isEmpty();
  }

  @Test
  @DisplayName(
      "AC-024-05 Contacts with a phone and notes but no e-mail, name pair or company give no set")
  void ac_024_05_without_identity_no_set() {
    contact("firstName", "Ana", "phone", "040 123 456", "notes", "Klicala");
    contact("firstName", "Ana", "phone", "040 123 456", "notes", "Klicala");

    assertThat(sets()).isEmpty();
  }

  @Test
  @DisplayName("AC-024-06 ana@acme.com in Acme and in Beta gives no set in Acme")
  void ac_024_06_other_group_not_compared() {
    contact("firstName", "Ana", "email", "ana@acme.com");
    Grp beta = group(ana, "Beta");
    createContact(ana, beta, "firstName", "Ana", "email", "ana@acme.com");

    assertThat(sets()).isEmpty();
  }

  @Test
  @DisplayName("AC-024-07 a duplicate Contact in Trash is in no set")
  void ac_024_07_trash_not_compared() {
    contact("firstName", "Ana", "email", "ana@acme.com");
    String gone = contact("firstName", "Anica", "email", "ana@acme.com");
    moveToTrash(ana, acme, gone);

    assertThat(sets()).isEmpty();
  }

  @Test
  @DisplayName("AC-024-08 the search changes no Contact")
  void ac_024_08_search_changes_nothing() {
    String a = contact("firstName", "Ana", "email", "ana@acme.com");
    String b = contact("firstName", "Anica", "email", "ana@acme.com");
    JsonNode beforeA = getContact(ana, acme, a);
    JsonNode beforeB = getContact(ana, acme, b);
    int entries = activity(ana, acme, "").size();

    sets();

    assertThat(getContact(ana, acme, a)).isEqualTo(beforeA);
    assertThat(getContact(ana, acme, b)).isEqualTo(beforeB);
    assertThat(activity(ana, acme, "")).hasSize(entries);
  }

  @Test
  @DisplayName("AC-024-09 a Viewer's duplicate search is refused")
  void ac_024_09_viewer_refused() {
    TestUser viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");

    viola.get(acme.duplicates()).expectError(403, "auth.role_forbidden");
  }

  @Test
  @DisplayName("AC-024-10 without duplicates the result is empty")
  void ac_024_10_no_duplicates() {
    contact("firstName", "Ana", "lastName", "Novak", "email", "ana@acme.com");
    contact("firstName", "Boris", "lastName", "Kos", "email", "boris@acme.com");

    assertThat(sets()).isEmpty();
  }

  @Test
  @DisplayName(
      "AC-024-11 a Contact of a certain and a probable set appears only in the certain set")
  void ac_024_11_certain_wins() {
    String a =
        contact(
            "firstName", "Ana", "lastName", "Novak", "company", "Acme", "email", "ana@acme.com");
    String b = contact("firstName", "Anica", "lastName", "Kos", "email", "ana@acme.com");
    contact("firstName", "Ana", "lastName", "Novak", "company", "Acme");

    List<JsonNode> withA = sets().stream().filter(s -> ids(s).contains(a)).toList();

    assertThat(withA).hasSize(1);
    assertThat(withA.get(0).path("mark").asText()).isEqualTo("certain");
    assertThat(ids(withA.get(0))).containsExactlyInAnyOrder(a, b);
  }

  @Test
  @DisplayName("EX-024-01 the names Škerlj and Skerlj are equal")
  void ex_024_01_accents_ignored() {
    contact("firstName", "Ana", "lastName", "Škerlj", "company", "Acme");
    contact("firstName", "Ana", "lastName", "Skerlj", "company", "Acme");

    List<JsonNode> sets = sets();

    assertThat(sets).hasSize(1);
    assertThat(sets.get(0).path("mark").asText()).isEqualTo("probable");
  }

  @Test
  @DisplayName("EX-024-02 the same name with empty companies and phones is no probable duplicate")
  void ex_024_02_name_needs_company_or_phone() {
    contact("firstName", "Ana", "lastName", "Novak");
    contact("firstName", "Ana", "lastName", "Novak");

    assertThat(sets()).isEmpty();
  }

  @Test
  @DisplayName("EX-024-03 two empty e-mails are no certain duplicate")
  void ex_024_03_empty_emails_not_equal() {
    contact("firstName", "Ana", "lastName", "Novak", "company", "Acme");
    contact("firstName", "Boris", "lastName", "Kos", "company", "Beta");

    assertThat(sets()).isEmpty();
  }

  @Test
  @DisplayName("EX-024-04 the same first name and company with another last name is no duplicate")
  void ex_024_04_different_last_name() {
    contact("firstName", "Ana", "lastName", "Novak", "company", "Acme");
    contact("firstName", "Ana", "lastName", "Kos", "company", "Acme");

    assertThat(sets()).isEmpty();
  }

  @Test
  @DisplayName("EX-024-05 the duplicate list of 10,000 Contacts is produced")
  void ex_024_05_ten_thousand_contacts() {
    Db.insertContacts(acme.id(), ana.userId(), 9_998);
    String a = contact("firstName", "Ana", "email", "ana@acme.com");
    String b = contact("firstName", "Anica", "email", "ana@acme.com");

    List<JsonNode> sets = sets();

    assertThat(sets).hasSize(1);
    assertThat(ids(sets.get(0))).containsExactlyInAnyOrder(a, b);
  }

  @Test
  @DisplayName(
      "EX-024-06 first name Ana only and the same company, without last names, is not matched")
  void ex_024_06_both_names_required() {
    contact("firstName", "Ana", "company", "Acme");
    contact("firstName", "Ana", "company", "Acme");

    assertThat(sets()).isEmpty();
  }
}
