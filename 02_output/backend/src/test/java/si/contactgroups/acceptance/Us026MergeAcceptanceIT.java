package si.contactgroups.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.contactgroups.acceptance.support.AcceptanceTestBase;
import si.contactgroups.acceptance.support.Grp;
import si.contactgroups.acceptance.support.Json;
import si.contactgroups.acceptance.support.Resp;
import si.contactgroups.acceptance.support.TestUser;

/**
 * US-026 Merge duplicate Contacts ({@code POST /groups/{g}/merges}). A is always the older Contact
 * and the Survivor.
 */
class Us026MergeAcceptanceIT extends AcceptanceTestBase {

  private TestUser ana;
  private TestUser boris;
  private Grp acme;

  @BeforeEach
  void borisEditsAcme() {
    ana = user("Ana Novak", "ana@acme.com");
    acme = group(ana, "Acme");
    boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
  }

  private String contact(Object... keyValues) {
    return createContact(ana, acme, keyValues).path("id").asText();
  }

  /** The merge request for the Contacts as {@code user} reads them now. */
  private ObjectNode request(
      TestUser user,
      String survivor,
      List<String> ids,
      ObjectNode fields,
      List<JsonNode> attributes) {
    ArrayNode contacts = Json.MAPPER.createArrayNode();
    for (String id : ids) {
      contacts.add(
          Json.obj("id", id, "version", getContact(user, acme, id).path("version").asInt()));
    }
    return Json.obj(
        "survivorId", survivor, "contacts", contacts, "fields", fields, "attributes", attributes);
  }

  private Resp merge(
      TestUser user,
      String survivor,
      List<String> ids,
      ObjectNode fields,
      List<JsonNode> attributes) {
    return user.post(acme.merges(), request(user, survivor, ids, fields, attributes));
  }

  private List<String> trashIds() {
    return Json.field(ana.get(acme.trash()).expect(200).json(), "id");
  }

  @Test
  @DisplayName("AC-026-02 the Survivor gets the Tags udelezenec2025 and neObvescaj of both")
  void ac_026_02_tags_combined() {
    String a =
        contact("firstName", "Ana", "email", "ana.n@acme.com", "tags", Json.arr("udelezenec2025"));
    String b =
        contact("firstName", "ana", "email", "ana.n@acme.com", "tags", Json.arr("neObvescaj"));

    merge(
            boris,
            a,
            List.of(a, b),
            Json.obj("firstName", "Ana", "email", "ana.n@acme.com"),
            List.of())
        .expect(200);

    assertThat(Json.strings(getContact(ana, acme, a).path("tags")))
        .containsExactlyInAnyOrder("udelezenec2025", "neObvescaj");
  }

  @Test
  @DisplayName("AC-026-04 the Survivor A holds the chosen values with a new version; B is in Trash")
  void ac_026_04_confirmed_merge() {
    String a = contact("firstName", "Ana", "lastName", "Novak", "email", "ana.n@acme.com");
    String b =
        contact(
            "firstName",
            "ana",
            "lastName",
            "Novak",
            "email",
            "ana.n@acme.com",
            "phone",
            "040 123 456");
    int versionBefore = getContact(ana, acme, a).path("version").asInt();

    var merged =
        merge(
                boris,
                a,
                List.of(a, b),
                Json.obj(
                    "firstName",
                    "Ana",
                    "lastName",
                    "Novak",
                    "email",
                    "ana.n@acme.com",
                    "phone",
                    "040 123 456"),
                List.of())
            .expect(200)
            .json();

    assertThat(merged.path("id").asText()).isEqualTo(a);
    JsonNode survivor = getContact(ana, acme, a);
    assertThat(survivor.path("firstName").asText()).isEqualTo("Ana");
    assertThat(survivor.path("phone").asText()).isEqualTo("040 123 456");
    assertThat(survivor.path("version").asInt()).isEqualTo(versionBefore + 1);
    assertThat(trashIds()).containsExactly(b);
    assertThat(Json.field(contactPage(ana, acme, "").path("items"), "id")).containsExactly(a);
  }

  @Test
  @DisplayName(
      "AC-026-06 a merged result without first name, last name, e-mail and company is refused")
  void ac_026_06_result_must_follow_br_06() {
    String a = contact("firstName", "Ana", "phone", "040 123 456");
    String b = contact("lastName", "Novak", "phone", "040 123 456");

    Resp answer =
        merge(
                boris,
                a,
                List.of(a, b),
                Json.obj("firstName", null, "lastName", null, "phone", "040 123 456"),
                List.of())
            .expect(422);

    assertThat(answer.errorCodes()).contains("validation.required_any");
    assertThat(trashIds()).isEmpty();
    assertThat(getContact(ana, acme, a).path("firstName").asText()).isEqualTo("Ana");
    assertThat(getContact(ana, acme, a).path("version").asInt()).isEqualTo(1);
  }

  @Test
  @DisplayName(
      "AC-026-07 one entry lists A and B and the chosen values; A's History shows B's earlier entries")
  void ac_026_07_merge_history() {
    String a = contact("firstName", "Ana", "email", "ana.n@acme.com");
    String b = contact("firstName", "Anica", "email", "ana.n@acme.com", "company", "Beta");
    edit(ana, acme, b, "city", "Celje");

    merge(
            boris,
            a,
            List.of(a, b),
            Json.obj("firstName", "Anica", "email", "ana.n@acme.com", "company", "Beta"),
            List.of())
        .expect(200);

    List<JsonNode> mergeEntries = activity(ana, acme, "action=CONTACTS_MERGED");
    assertThat(mergeEntries).hasSize(1);
    String details = mergeEntries.get(0).path("details").toString();
    assertThat(details).contains(a).contains(b).contains("Anica").contains("Beta");
    List<JsonNode> survivorHistory = history(ana, acme, a);
    assertThat(actions(survivorHistory)).contains("CONTACTS_MERGED");
    assertThat(survivorHistory.stream().map(e -> e.path("contactId").asText())).contains(b);
    assertThat(
            survivorHistory.stream()
                .filter(e -> e.path("contactId").asText().equals(b))
                .map(e -> e.path("action").asText()))
        .contains("CONTACT_CREATED", "CONTACT_EDITED");
  }

  @Test
  @DisplayName("AC-026-08 a merge after Boris edited B is refused; nothing is merged")
  void ac_026_08_changed_contact_refused() {
    String a = contact("firstName", "Ana", "email", "ana.n@acme.com");
    String b = contact("firstName", "Anica", "email", "ana.n@acme.com");
    ObjectNode opened =
        request(
            ana,
            a,
            List.of(a, b),
            Json.obj("firstName", "Ana", "email", "ana.n@acme.com"),
            List.of());

    edit(boris, acme, b, "city", "Celje");
    ana.post(acme.merges(), opened).expectError(409, "contact.changed_by_someone_else");

    assertThat(trashIds()).isEmpty();
    assertThat(getContact(ana, acme, a).path("version").asInt()).isEqualTo(1);
  }

  @Test
  @DisplayName("AC-026-09 a merge of one Contact and a merge across two Groups are refused")
  void ac_026_09_invalid_selection_refused() {
    String a = contact("firstName", "Ana", "email", "ana.n@acme.com");
    Grp beta = group(ana, "Beta");
    String other =
        createContact(ana, beta, "firstName", "Ana", "email", "ana.n@acme.com").path("id").asText();
    ObjectNode one =
        Json.obj(
            "survivorId", a,
            "contacts", Json.arr(Json.obj("id", a, "version", 1)),
            "fields", Json.obj("firstName", "Ana"),
            "attributes", Json.arr());
    ObjectNode across =
        Json.obj(
            "survivorId", a,
            "contacts",
                Json.arr(Json.obj("id", a, "version", 1), Json.obj("id", other, "version", 1)),
            "fields", Json.obj("firstName", "Ana", "email", "ana.n@acme.com"),
            "attributes", Json.arr());

    Resp single = ana.post(acme.merges(), one);
    Resp twoGroups = ana.post(acme.merges(), across);

    assertThat(single.status()).isIn(400, 422);
    assertThat(twoGroups.status()).isIn(404, 422);
    assertThat(trashIds()).isEmpty();
    assertThat(contactPage(ana, beta, "").path("total").asInt()).isEqualTo(1);
    assertThat(getContact(ana, acme, a).path("version").asInt()).isEqualTo(1);
  }

  @Test
  @DisplayName("AC-026-10 B, merged 3 days ago, is back in the list after a restore")
  void ac_026_10_merged_away_contact_restorable() {
    String a = contact("firstName", "Ana", "email", "ana.n@acme.com");
    String b = contact("firstName", "Anica", "email", "ana.n@acme.com");
    merge(
            boris,
            a,
            List.of(a, b),
            Json.obj("firstName", "Ana", "email", "ana.n@acme.com"),
            List.of())
        .expect(200);
    clock.advance(Duration.ofDays(3));

    boris.post(acme.restore(b)).expect(200);

    assertThat(Json.field(contactPage(ana, acme, "").path("items"), "id"))
        .containsExactlyInAnyOrder(a, b);
  }

  @Test
  @DisplayName("AC-026-11 a Viewer's merge is refused")
  void ac_026_11_viewer_refused() {
    String a = contact("firstName", "Ana", "email", "ana.n@acme.com");
    String b = contact("firstName", "Anica", "email", "ana.n@acme.com");
    TestUser viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");

    merge(
            viola,
            a,
            List.of(a, b),
            Json.obj("firstName", "Ana", "email", "ana.n@acme.com"),
            List.of())
        .expectError(403, "auth.role_forbidden");

    assertThat(trashIds()).isEmpty();
  }
}
