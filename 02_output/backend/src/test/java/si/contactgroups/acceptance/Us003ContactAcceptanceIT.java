package si.contactgroups.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import si.contactgroups.acceptance.support.AcceptanceTestBase;
import si.contactgroups.acceptance.support.Grp;
import si.contactgroups.acceptance.support.Json;
import si.contactgroups.acceptance.support.Resp;
import si.contactgroups.acceptance.support.TestUser;

/** US-003 Create, view and edit a Contact (BR-06, BR-07). */
class Us003ContactAcceptanceIT extends AcceptanceTestBase {

  private TestUser ana;
  private Grp acme;

  @BeforeEach
  void anaOwnsAcme() {
    ana = user("Ana Novak", "ana@acme.com");
    acme = group(ana, "Acme");
  }

  private long total() {
    return contactPage(ana, acme, "").path("total").asLong();
  }

  private Resp post(Object... keyValues) {
    return ana.post(acme.contacts(), contactBody(keyValues));
  }

  @Test
  @DisplayName("AC-003-01 a new Contact is stored with version 1 and shown")
  void ac_003_01_create_contact_version_1() {
    var created =
        post("firstName", "Ana", "lastName", "Novak", "email", "ana@acme.com", "company", "Acme d.o.o.")
            .expect(201)
            .json();

    assertThat(created.path("version").asInt()).isEqualTo(1);
    JsonNode read = getContact(ana, acme, created.path("id").asText());
    assertThat(read.path("firstName").asText()).isEqualTo("Ana");
    assertThat(read.path("lastName").asText()).isEqualTo("Novak");
    assertThat(read.path("email").asText()).isEqualTo("ana@acme.com");
    assertThat(read.path("company").asText()).isEqualTo("Acme d.o.o.");
    assertThat(read.path("version").asInt()).isEqualTo(1);
    assertThat(total()).isEqualTo(1);
  }

  @Test
  @DisplayName("AC-003-02 a Contact with only a phone is refused: one of the four fields is needed")
  void ac_003_02_one_of_four_fields_required() {
    var answer = post("phone", "040 123 456").expect(422);

    assertThat(answer.errorCodes()).contains("validation.required_any");
    assertThat(total()).isZero();
  }

  @Test
  @DisplayName("AC-003-03 the e-mail 'ana@' is refused with an error on the e-mail field")
  void ac_003_03_invalid_email_refused() {
    var answer = post("firstName", "Ana", "email", "ana@").expect(422);

    assertThat(answer.errorFields()).contains("email");
    assertThat(answer.errorCodes()).contains("validation.email_invalid");
    assertThat(total()).isZero();
  }

  @Test
  @DisplayName("AC-003-04 the e-mail ANA@ACME.COM is stored as ana@acme.com")
  void ac_003_04_email_lowercase() {
    var created = post("email", "ANA@ACME.COM").expect(201).json();

    assertThat(created.path("email").asText()).isEqualTo("ana@acme.com");
    assertThat(getContact(ana, acme, created.path("id").asText()).path("email").asText())
        .isEqualTo("ana@acme.com");
  }

  @Test
  @DisplayName("AC-003-05 changing the company to Beta d.o.o. gives version 2")
  void ac_003_05_edit_raises_version() {
    var created = createContact(ana, acme, "firstName", "Ana", "lastName", "Novak", "company", "Acme d.o.o.");

    var saved = edit(ana, acme, created.path("id").asText(), "company", "Beta d.o.o.");

    assertThat(saved.path("company").asText()).isEqualTo("Beta d.o.o.");
    assertThat(saved.path("version").asInt()).isEqualTo(2);
    var read = getContact(ana, acme, created.path("id").asText());
    assertThat(read.path("company").asText()).isEqualTo("Beta d.o.o.");
    assertThat(read.path("version").asInt()).isEqualTo(2);
  }

  @Test
  @DisplayName("AC-003-06 a save with a stale version is refused and the current Contact is returned")
  void ac_003_06_stale_version_refused() {
    TestUser boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
    var created = createContact(ana, acme, "firstName", "Ana", "lastName", "Novak", "company", "Acme d.o.o.");
    String id = created.path("id").asText();
    edit(ana, acme, id, "jobTitle", "Vodja");
    JsonNode anaSees = getContact(ana, acme, id);
    JsonNode borisSees = getContact(boris, acme, id);
    assertThat(anaSees.path("version").asInt()).isEqualTo(2);

    save(boris, acme, borisSees, "city", "Maribor").expect(200);
    var refused = save(ana, acme, anaSees, "company", "Beta d.o.o.").expectError(409, "contact.changed_by_someone_else");

    JsonNode current = refused.json().path("current");
    assertThat(current.path("version").asInt()).isEqualTo(3);
    assertThat(current.path("city").asText()).isEqualTo("Maribor");
    assertThat(current.path("company").asText()).isEqualTo("Acme d.o.o.");
    JsonNode stored = getContact(ana, acme, id);
    assertThat(stored.path("company").asText()).isEqualTo("Acme d.o.o.");
    assertThat(stored.path("version").asInt()).isEqualTo(3);
  }

  @Test
  @DisplayName("AC-003-07 a Viewer cannot create a Contact")
  void ac_003_07_viewer_cannot_create() {
    TestUser viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");

    viola.post(acme.contacts(), contactBody("firstName", "Ana")).expectError(403, "auth.role_forbidden");

    assertThat(total()).isZero();
  }

  @Test
  @DisplayName("AC-003-08 a Member sees all 11 predefined fields, the Tag and the Attribute")
  void ac_003_08_member_sees_all_fields() {
    TestUser viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");
    var created =
        createContact(
            ana,
            acme,
            "firstName", "Ana",
            "lastName", "Novak",
            "email", "ana@acme.com",
            "phone", "040 123 456",
            "company", "Acme d.o.o.",
            "jobTitle", "Vodja",
            "street", "Slovenska cesta 1",
            "postalCode", "1000",
            "city", "Ljubljana",
            "country", "SI",
            "notes", "Udeleženka",
            "tags", Json.arr("udelezenec2025"),
            "attributes", attrs("kompetenca", "spletnoProgramiranje"));

    JsonNode read = getContact(viola, acme, created.path("id").asText());

    assertThat(read.path("firstName").asText()).isEqualTo("Ana");
    assertThat(read.path("lastName").asText()).isEqualTo("Novak");
    assertThat(read.path("email").asText()).isEqualTo("ana@acme.com");
    assertThat(read.path("phone").asText()).isEqualTo("040 123 456");
    assertThat(read.path("company").asText()).isEqualTo("Acme d.o.o.");
    assertThat(read.path("jobTitle").asText()).isEqualTo("Vodja");
    assertThat(read.path("street").asText()).isEqualTo("Slovenska cesta 1");
    assertThat(read.path("postalCode").asText()).isEqualTo("1000");
    assertThat(read.path("city").asText()).isEqualTo("Ljubljana");
    assertThat(read.path("country").asText()).isEqualTo("SI");
    assertThat(read.path("notes").asText()).isEqualTo("Udeleženka");
    assertThat(Json.strings(read.path("tags"))).containsExactly("udelezenec2025");
    assertThat(Json.field(read.path("attributes"), "key")).containsExactly("kompetenca");
    assertThat(Json.field(read.path("attributes"), "value")).containsExactly("spletnoProgramiranje");
  }

  @Test
  @DisplayName("AC-003-09 a second Contact with the same e-mail is stored")
  void ac_003_09_duplicate_email_allowed() {
    createContact(ana, acme, "email", "ana@acme.com");

    post("email", "ana@acme.com").expect(201);

    assertThat(total()).isEqualTo(2);
  }

  @Test
  @DisplayName("AC-003-10 a save without any change keeps version 2")
  void ac_003_10_save_without_change_keeps_version() {
    var created = createContact(ana, acme, "firstName", "Ana", "lastName", "Novak");
    String id = created.path("id").asText();
    edit(ana, acme, id, "company", "Acme");

    var saved = save(ana, acme, getContact(ana, acme, id)).expect(200).json();

    assertThat(saved.path("version").asInt()).isEqualTo(2);
    assertThat(getContact(ana, acme, id).path("version").asInt()).isEqualTo(2);
  }

  @Test
  @DisplayName("EX-003-01 only the first name Ana is enough")
  void ex_003_01_only_first_name() {
    post("firstName", "Ana").expect(201);
  }

  @Test
  @DisplayName("EX-003-02 only the company Acme is enough")
  void ex_003_02_only_company() {
    post("company", "Acme").expect(201);
  }

  @Test
  @DisplayName("EX-003-03 a first name of spaces is stored empty")
  void ex_003_03_spaces_trimmed_to_empty() {
    var created = post("firstName", "   ", "lastName", "Novak").expect(201).json();

    JsonNode read = getContact(ana, acme, created.path("id").asText());
    assertThat(read.path("firstName").asText("")).isEmpty();
    assertThat(read.path("lastName").asText()).isEqualTo("Novak");
  }

  @ParameterizedTest(name = "EX-003-04 first name of {0} characters: stored = {1}")
  @CsvSource({"255, true", "256, false"})
  void ex_003_04_first_name_length(int length, boolean stored) {
    var answer = post("firstName", chars(length, 'a'));

    assertThat(answer.status()).isEqualTo(stored ? 201 : 422);
    assertThat(total()).isEqualTo(stored ? 1 : 0);
  }

  @ParameterizedTest(name = "EX-003-05 notes of {0} characters: stored = {1}")
  @CsvSource({"5000, true", "5001, false"})
  void ex_003_05_notes_length(int length, boolean stored) {
    var answer = post("firstName", "Ana", "notes", chars(length, 'n'));

    assertThat(answer.status()).isEqualTo(stored ? 201 : 422);
    if (!stored) {
      assertThat(answer.errorFields()).contains("notes");
    }
    assertThat(total()).isEqualTo(stored ? 1 : 0);
  }

  @ParameterizedTest(name = "EX-003-06 e-mail of {0} characters: stored = {1}")
  @CsvSource({"255, true", "256, false"})
  void ex_003_06_email_length(int length, boolean stored) {
    var answer = post("email", emailOfLength(length));

    assertThat(answer.status()).isEqualTo(stored ? 201 : 422);
    assertThat(total()).isEqualTo(stored ? 1 : 0);
  }

  @Test
  @DisplayName("EX-003-07 the phone 'abc' is refused")
  void ex_003_07_phone_letters_refused() {
    var answer = post("firstName", "Ana", "phone", "abc").expect(422);

    assertThat(answer.errorFields()).contains("phone");
    assertThat(answer.errorCodes()).contains("validation.phone_invalid");
    assertThat(total()).isZero();
  }

  @ParameterizedTest(name = "EX-003-08 phone ''{0}'': stored = {1}")
  @CsvSource({
    "'+386 (40) 123-456', true",
    "'123456789012345678901234567890', true",
    "'1234567890123456789012345678901', false"
  })
  void ex_003_08_phone_characters_and_length(String phone, boolean stored) {
    var answer = post("firstName", "Ana", "phone", phone);

    assertThat(answer.status()).isEqualTo(stored ? 201 : 422);
    if (stored) {
      assertThat(answer.json().path("phone").asText()).isEqualTo(phone);
    }
    assertThat(total()).isEqualTo(stored ? 1 : 0);
  }

  @ParameterizedTest(name = "EX-003-09 country {0}: stored = {1}")
  @CsvSource({"SI, true", "XX, false"})
  void ex_003_09_country_on_the_list(String country, boolean stored) {
    var answer = post("firstName", "Ana", "country", country);

    assertThat(answer.status()).isEqualTo(stored ? 201 : 422);
    if (!stored) {
      assertThat(answer.errorCodes()).contains("validation.country_invalid");
    }
    assertThat(total()).isEqualTo(stored ? 1 : 0);
  }

  @Test
  @DisplayName("EX-003-10 the names Škerlj, Čeh and Žagar are stored unchanged")
  void ex_003_10_slovene_letters_unchanged() {
    var created = post("firstName", "Čeh", "lastName", "Škerlj", "company", "Žagar").expect(201).json();

    JsonNode read = getContact(ana, acme, created.path("id").asText());
    assertThat(read.path("firstName").asText()).isEqualTo("Čeh");
    assertThat(read.path("lastName").asText()).isEqualTo("Škerlj");
    assertThat(read.path("company").asText()).isEqualTo("Žagar");
  }

  @Test
  @DisplayName("EX-003-11 a save with an older version after two other saves is refused")
  void ex_003_11_old_version_after_two_saves() {
    var created = createContact(ana, acme, "firstName", "Ana");
    String id = created.path("id").asText();
    edit(ana, acme, id, "lastName", "Novak");
    edit(ana, acme, id, "company", "Acme");

    save(ana, acme, created, "city", "Kranj").expectError(409, "contact.changed_by_someone_else");

    JsonNode read = getContact(ana, acme, id);
    assertThat(read.path("version").asInt()).isEqualTo(3);
    assertThat(read.path("city").asText("")).isEmpty();
  }

  @Test
  @DisplayName("EX-003-12 a Member of another Group gets not found for this Contact")
  void ex_003_12_member_of_other_group_not_found() {
    TestUser boris = user("Boris Kos", "boris@acme.com");
    group(boris, "Beta");
    var created = createContact(ana, acme, "firstName", "Ana", "lastName", "Novak");

    var answer = boris.get(acme.contact(created.path("id").asText())).expect(404);

    assertThat(answer.body()).doesNotContain("Novak");
  }

  @Test
  @DisplayName("EX-003-13 the last name <script>alert(1)</script> is stored as plain text")
  void ex_003_13_script_stored_as_text() {
    var created = post("lastName", "<script>alert(1)</script>").expect(201).json();

    assertThat(getContact(ana, acme, created.path("id").asText()).path("lastName").asText())
        .isEqualTo("<script>alert(1)</script>");
  }
}
