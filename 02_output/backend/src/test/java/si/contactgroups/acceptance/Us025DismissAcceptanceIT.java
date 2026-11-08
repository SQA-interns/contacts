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
import si.contactgroups.acceptance.support.Resp;
import si.contactgroups.acceptance.support.TestUser;

/** US-025 Mark a Duplicate set as "not a duplicate". */
class Us025DismissAcceptanceIT extends AcceptanceTestBase {

  private TestUser ana;
  private TestUser boris;
  private Grp acme;

  @BeforeEach
  void borisEditsAcme() {
    ana = user("Ana Novak", "ana@acme.com");
    acme = group(ana, "Acme");
    boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
  }

  /** A Contact Ana Novak of Acme d.o.o. (probable duplicates of each other). */
  private String anaNovak(String city) {
    return createContact(
            ana,
            acme,
            "firstName",
            "Ana",
            "lastName",
            "Novak",
            "company",
            "Acme d.o.o.",
            "city",
            city)
        .path("id")
        .asText();
  }

  private String sharedEmail(String firstName) {
    return createContact(ana, acme, "firstName", firstName, "email", "skupna@acme.com")
        .path("id")
        .asText();
  }

  private List<List<String>> sets() {
    return Json.list(ana.get(acme.duplicates()).expect(200).json()).stream()
        .map(s -> Json.field(s.path("contacts"), "id"))
        .toList();
  }

  private Resp dismiss(TestUser user, String... ids) {
    return user.post(acme.dismissals(), Json.obj("contactIds", List.of(ids)));
  }

  @Test
  @DisplayName("AC-025-01 a set {A, B} marked 'not a duplicate' disappears from the list")
  void ac_025_01_dismissed_set_disappears() {
    String a = anaNovak("Kranj");
    String b = anaNovak("Celje");
    assertThat(sets()).hasSize(1);

    dismiss(boris, a, b).expect(204);

    assertThat(sets()).isEmpty();
  }

  @Test
  @DisplayName("AC-025-02 when the search runs again {A, B} is not shown")
  void ac_025_02_not_shown_again() {
    String a = anaNovak("Kranj");
    String b = anaNovak("Celje");
    dismiss(boris, a, b).expect(204);

    sets();

    assertThat(sets()).noneMatch(set -> set.containsAll(List.of(a, b)) && set.size() == 2);
    assertThat(sets()).isEmpty();
  }

  @Test
  @DisplayName("AC-025-03 a new Contact C matching A and B makes {A, B, C} shown")
  void ac_025_03_bigger_set_shown() {
    String a = anaNovak("Kranj");
    String b = anaNovak("Celje");
    dismiss(boris, a, b).expect(204);

    String c = anaNovak("Koper");

    assertThat(sets()).hasSize(1);
    assertThat(sets().get(0)).containsExactlyInAnyOrder(a, b, c);
  }

  @Test
  @DisplayName("AC-025-04 a Viewer's dismiss request is refused")
  void ac_025_04_viewer_refused() {
    String a = anaNovak("Kranj");
    String b = anaNovak("Celje");
    TestUser viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");

    dismiss(viola, a, b).expectError(403, "auth.role_forbidden");

    assertThat(sets()).hasSize(1);
  }

  @Test
  @DisplayName("AC-025-05 after dismissing, A and B are unchanged and have no new Version")
  void ac_025_05_contacts_unchanged() {
    String a = anaNovak("Kranj");
    String b = anaNovak("Celje");
    JsonNode beforeA = getContact(ana, acme, a);
    JsonNode beforeB = getContact(ana, acme, b);

    dismiss(boris, a, b).expect(204);

    assertThat(getContact(ana, acme, a)).isEqualTo(beforeA);
    assertThat(getContact(ana, acme, b)).isEqualTo(beforeB);
    assertThat(Json.list(ana.get(acme.versions(a)).expect(200).json())).hasSize(1);
    assertThat(Json.list(ana.get(acme.versions(b)).expect(200).json())).hasSize(1);
  }

  @Test
  @DisplayName(
      "EX-025-01 after one Contact of a dismissed set is deleted, a different set of the rest is shown")
  void ex_025_01_different_set_after_deletion() {
    String a = sharedEmail("A");
    String b = sharedEmail("B");
    String c = sharedEmail("C");
    dismiss(boris, a, b, c).expect(204);
    moveToTrash(ana, acme, c);

    String d = sharedEmail("D");

    assertThat(sets()).hasSize(1);
    assertThat(sets().get(0)).containsExactlyInAnyOrder(a, b, d);
  }

  @Test
  @DisplayName(
      "EX-025-02 a dismissed set of 3 with one edited so it no longer matches shows nothing")
  void ex_025_02_rest_of_dismissed_set_not_shown() {
    String a = sharedEmail("A");
    String b = sharedEmail("B");
    String c = sharedEmail("C");
    dismiss(boris, a, b, c).expect(204);

    edit(ana, acme, c, "email", "druga@acme.com");

    assertThat(sets()).isEmpty();
  }

  @Test
  @DisplayName("EX-025-03 dismissing a set that does not exist or is of another Group is not found")
  void ex_025_03_unknown_set_not_found() {
    Grp beta = group(ana, "Beta");
    String x =
        createContact(ana, beta, "firstName", "X", "email", "x@acme.com").path("id").asText();
    String y =
        createContact(ana, beta, "firstName", "Y", "email", "x@acme.com").path("id").asText();

    dismiss(ana, x, y).expect(404);
    dismiss(ana, Db.randomId(), Db.randomId()).expect(404);

    assertThat(Json.list(ana.get(beta.duplicates()).expect(200).json())).hasSize(1);
  }

  @Test
  @DisplayName(
      "EX-025-04 two Editors dismissing the same set at once both succeed; the set stays hidden")
  void ex_025_04_concurrent_dismissals() throws Exception {
    String a = anaNovak("Kranj");
    String b = anaNovak("Celje");

    List<Resp> answers = atOnce(() -> dismiss(boris, a, b), () -> dismiss(ana, a, b));

    assertThat(answers).allMatch(r -> r.status() == 204);
    assertThat(sets()).isEmpty();
  }
}
