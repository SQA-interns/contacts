package si.contactgroups.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import si.contactgroups.acceptance.support.AcceptanceTestBase;
import si.contactgroups.acceptance.support.Grp;
import si.contactgroups.acceptance.support.Json;
import si.contactgroups.acceptance.support.TestUser;

/** F-41 (fix loop 1): R-003-02, a Contact has exactly the predefined fields; invalid input. */
class FixRun1ContactIT extends AcceptanceTestBase {

  private TestUser ana;
  private Grp acme;

  @BeforeEach
  void anaOwnsAcme() {
    ana = user("Ana Novak", "ana@acme.com");
    acme = group(ana, "Acme");
  }

  private int stored() {
    return contactPage(ana, acme, "").path("total").asInt();
  }

  @ParameterizedTest(name = "R-003-02 a field named {0} is not offered and the Contact is refused")
  @ValueSource(strings = {"fax", "mobile", "title", "website", "name", "id", "version2", "Email"})
  void an_extra_field_is_refused(String field) {
    ana.post(acme.contacts(), Json.obj("firstName", "Ana", field, "x")).expect(400);

    assertThat(stored()).isZero();
  }

  @Test
  @DisplayName("R-003-02 a field of the wrong JSON type is refused, nothing is stored")
  void a_field_of_the_wrong_type_is_refused() {
    ana.post(acme.contacts(), Json.parse("{\"firstName\":\"Ana\",\"phone\":12345}")).expect(400);
    ana.post(acme.contacts(), Json.parse("{\"firstName\":[\"Ana\"]}")).expect(400);
    ana.post(acme.contacts(), Json.parse("{\"firstName\":{\"a\":1}}")).expect(400);
    ana.post(acme.contacts(), Json.parse("{\"firstName\":true}")).expect(400);

    assertThat(stored()).isZero();
  }

  @Test
  @DisplayName("R-003-02 the answer carries no field beyond the 11 and the contract ones")
  void the_answer_names_no_other_field() {
    JsonNode created = createContact(ana, acme, "firstName", "Ana");

    java.util.Set<String> names = new java.util.TreeSet<>();
    created.fieldNames().forEachRemaining(names::add);
    java.util.Set<String> expected = new java.util.TreeSet<>(FIELDS);
    expected.addAll(
        java.util.List.of("id", "version", "tags", "attributes", "createdAt", "updatedAt"));
    assertThat(expected).containsAll(names);
  }

  @Test
  @DisplayName("R-003-02 an edit with an extra field is refused and the Contact stays")
  void an_edit_with_an_extra_field_is_refused() {
    JsonNode current = createContact(ana, acme, "firstName", "Ana");

    save(ana, acme, current, "fax", "x").expect(400);

    assertThat(getContact(ana, acme, current.path("id").asText()).path("version").asInt())
        .isEqualTo(1);
  }

  @Test
  @DisplayName("R-003-02 a body that is not a JSON object is refused")
  void a_body_that_is_not_an_object_is_refused() {
    ana.post(acme.contacts(), Json.parse("[]")).expect(400);
    ana.post(acme.contacts(), Json.parse("\"Ana\"")).expect(400);
    ana.post(acme.contacts(), Json.parse("null")).expect(400);

    assertThat(stored()).isZero();
  }

  @Test
  @DisplayName("R-003-02 a country code that is not on the list or in lower case is refused")
  void a_country_off_the_list_is_refused() {
    for (String code : new String[] {"XX", "ZZ", "si", "SVN", "S", ""}) {
      if (code.isEmpty()) {
        continue;
      }
      ana.post(acme.contacts(), Json.obj("firstName", "Ana", "country", code)).expect(422);
    }

    assertThat(stored()).isZero();
  }
}
