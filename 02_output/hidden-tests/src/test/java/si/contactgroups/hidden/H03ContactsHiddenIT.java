package si.contactgroups.hidden;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Hidden cases of US-003, US-004, US-005, US-006, BR-06 and BR-07. */
class H03ContactsHiddenIT extends HiddenBase {

  U ana;
  G acme;

  @BeforeEach
  void setUp() {
    ana = user("Ana Novak", "ana@acme.com");
    acme = group(ana, "Acme");
  }

  @Test
  @DisplayName("R-003-01 other a Contact with all 11 fields is stored with version 1")
  void r00301() {
    String c =
        contact(ana, acme, "firstName", "Mojca", "lastName", "Kovač", "email", "mojca@kovac.si", "phone", "01 234 56 78",
            "company", "Kovač s.p.", "jobTitle", "Direktorica", "street", "Trubarjeva 5", "postalCode", "3000",
            "city", "Celje", "country", "AT", "notes", "Opomba");
    JsonNode r = read(ana, acme, c);
    assertThat(r.path("version").asInt()).isEqualTo(1);
    assertThat(r.path("country").asText()).isEqualTo("AT");
    assertThat(r.path("jobTitle").asText()).isEqualTo("Direktorica");
  }

  @Test
  @DisplayName("R-003-02 invalid an unknown field middleName is refused and nothing is stored")
  void r00302() {
    Resp r = ana.post(acme.contacts(), obj("firstName", "Mojca", "middleName", "X"));
    assertThat(r.status()).isEqualTo(400);
    assertThat(total(ana, acme, "")).isZero();
  }

  @Test
  @DisplayName("R-003-03 combination an invalid e-mail and an invalid phone are both reported; nothing stored")
  void r00303() {
    Resp r = ana.post(acme.contacts(), obj("firstName", "Mojca", "email", "mojca@@kovac", "phone", "01#234")).is(422);
    assertThat(r.fieldCodes()).contains("validation.email_invalid", "validation.phone_invalid");
    assertThat(total(ana, acme, "")).isZero();
  }

  @Test
  @DisplayName("R-003-04 other an edit to MOJCA@Kovac.SI stores mojca@kovac.si")
  void r00304() {
    String c = contact(ana, acme, "firstName", "Mojca");
    assertThat(edit(ana, acme, c, "email", "MOJCA@Kovac.SI").path("email").asText()).isEqualTo("mojca@kovac.si");
  }

  @Test
  @DisplayName("R-003-05 other the company '  Kovač s.p.  ' is stored trimmed")
  void r00305() {
    String c = contact(ana, acme, "company", "  Kovač s.p.  ");
    assertThat(read(ana, acme, c).path("company").asText()).isEqualTo("Kovač s.p.");
  }

  @Test
  @DisplayName("R-003-06 boundary three saves in a row give version 4")
  void r00306() {
    String c = contact(ana, acme, "firstName", "Mojca");
    edit(ana, acme, c, "city", "A");
    edit(ana, acme, c, "city", "B");
    assertThat(edit(ana, acme, c, "city", "C").path("version").asInt()).isEqualTo(4);
  }

  @Test
  @DisplayName("R-003-07 combination a stale save with new Tags is refused and stores no Tag")
  void r00307() {
    String c = contact(ana, acme, "firstName", "Mojca");
    JsonNode old = read(ana, acme, c);
    edit(ana, acme, c, "city", "Celje");
    Resp r = save(ana, acme, old, "tags", arr("gost")).is(409, "contact.changed_by_someone_else");
    assertThat(r.json().path("current").path("city").asText()).isEqualTo("Celje");
    assertThat(read(ana, acme, c).path("tags")).isEmpty();
  }

  @Test
  @DisplayName("R-003-08 role a Viewer reads the Attributes and Tags of a Contact")
  void r00308() {
    String c = contact(ana, acme, "firstName", "Mojca", "tags", arr("a1"), "attributes", attrs("jezik", "nemščina"));
    U viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");
    JsonNode r = read(viola, acme, c);
    assertThat(texts(r.path("tags"), null)).containsExactly("a1");
    assertThat(texts(r.path("attributes"), "value")).containsExactly("nemščina");
  }

  @Test
  @DisplayName("R-003-09 role a Viewer's edit is refused and changes nothing")
  void r00309() {
    String c = contact(ana, acme, "firstName", "Mojca");
    U viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");
    save(viola, acme, read(viola, acme, c), "firstName", "Viola").is(403);
    assertThat(read(ana, acme, c).path("firstName").asText()).isEqualTo("Mojca");
  }

  @Test
  @DisplayName("R-003-10 other three Contacts with the same e-mail are all stored")
  void r00310() {
    for (int i = 0; i < 3; i++) {
      contact(ana, acme, "email", "skupna@acme.com");
    }
    assertThat(total(ana, acme, "")).isEqualTo(3);
  }

  @Test
  @DisplayName("R-003-11 other a save that only adds spaces around the name changes nothing")
  void r00311() {
    String c = contact(ana, acme, "firstName", "Mojca");
    assertThat(edit(ana, acme, c, "firstName", "  Mojca ").path("version").asInt()).isEqualTo(1);
    assertThat(history(ana, acme, c)).hasSize(1);
  }

  @Test
  @DisplayName("R-004-01 other three Attributes are stored with the Contact")
  void r00401() {
    String c = contact(ana, acme, "firstName", "M", "attributes", attrs("a", "1", "b", "2", "c", "3"));
    assertThat(texts(read(ana, acme, c).path("attributes"), "key")).containsExactlyInAnyOrder("a", "b", "c");
  }

  @Test
  @DisplayName("R-004-02 boundary a value of 1 character and a key of 49 are stored; a value of 300 is refused")
  void r00402() {
    String c = contact(ana, acme, "firstName", "M", "attributes", attrs(chars(49, 'k'), "x"));
    assertThat(read(ana, acme, c).path("attributes")).hasSize(1);
    save(ana, acme, read(ana, acme, c), "attributes", attrs("k", chars(300, 'v'))).is(422);
    assertThat(texts(read(ana, acme, c).path("attributes"), "value")).containsExactly("x");
  }

  @Test
  @DisplayName("R-004-03 combination the keys Jezik and JEZIK in one request are refused")
  void r00403() {
    ana.post(acme.contacts(), obj("firstName", "M", "attributes", attrs("Jezik", "a", "JEZIK", "b"))).is(422);
    assertThat(total(ana, acme, "")).isZero();
  }

  @Test
  @DisplayName("R-004-04 boundary 49 Attributes plus 2 are refused")
  void r00404() {
    List<JsonNode> a = new ArrayList<>();
    for (int i = 0; i < 51; i++) {
      a.add(obj("key", "k" + i, "value", "v"));
    }
    String c = contact(ana, acme, "firstName", "M", "attributes", a.subList(0, 49));
    save(ana, acme, read(ana, acme, c), "attributes", a).is(422);
    assertThat(read(ana, acme, c).path("attributes")).hasSize(49);
  }

  @Test
  @DisplayName("R-004-05 other the keys of two Contacts are offered once each")
  void r00405() {
    contact(ana, acme, "firstName", "A", "attributes", attrs("jezik", "a"));
    contact(ana, acme, "firstName", "B", "attributes", attrs("jezik", "b", "hobi", "c"));
    assertThat(texts(ana.get(acme.p() + "/facets").is(200).json().path("attributeKeys"), null))
        .containsExactlyInAnyOrder("jezik", "hobi");
  }

  @Test
  @DisplayName("R-004-06 other adding an Attribute records key and value")
  void r00406() {
    String c = contact(ana, acme, "firstName", "M");
    edit(ana, acme, c, "attributes", attrs("hobi", "šah"));
    JsonNode e = history(ana, acme, c).get(0);
    assertThat(e.path("action").asText()).isEqualTo("ATTRIBUTE_ADDED");
    assertThat(e.path("details").toString()).contains("hobi").contains("šah");
  }

  @Test
  @DisplayName("R-004-07 other changing an Attribute raises the version")
  void r00407() {
    String c = contact(ana, acme, "firstName", "M", "attributes", attrs("hobi", "šah"));
    assertThat(edit(ana, acme, c, "attributes", attrs("hobi", "tenis")).path("version").asInt()).isEqualTo(2);
  }

  @Test
  @DisplayName("R-004-08 role a Viewer's new Attribute is refused")
  void r00408() {
    String c = contact(ana, acme, "firstName", "M");
    U viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");
    save(viola, acme, read(viola, acme, c), "attributes", attrs("hobi", "šah")).is(403);
    assertThat(read(ana, acme, c).path("attributes")).isEmpty();
  }

  @Test
  @DisplayName("R-004-09 other the value 007 stays the text 007")
  void r00409() {
    String c = contact(ana, acme, "firstName", "M", "attributes", attrs("koda", "007"));
    assertThat(read(ana, acme, c).path("attributes").get(0).path("value").asText()).isEqualTo("007");
  }

  @Test
  @DisplayName("R-005-01 other two new Tags in one save are created and put on")
  void r00501() {
    String c = contact(ana, acme, "firstName", "M", "tags", arr("prvi", "drugi"));
    assertThat(texts(read(ana, acme, c).path("tags"), null)).containsExactlyInAnyOrder("prvi", "drugi");
    assertThat(tags(ana, acme)).hasSize(2);
  }

  @Test
  @DisplayName("R-005-02 invalid the Tag names 'a b' and 'tag!' are refused; 50 letters ž are accepted")
  void r00502() {
    ana.post(acme.contacts(), obj("firstName", "M", "tags", arr("a b"))).is(422);
    ana.post(acme.contacts(), obj("firstName", "M", "tags", arr("tag!"))).is(422);
    ana.post(acme.contacts(), obj("firstName", "M", "tags", arr(chars(50, 'ž')))).is(201);
    assertThat(total(ana, acme, "")).isEqualTo(1);
  }

  @Test
  @DisplayName("R-005-03 other NEOBVESCAJ reuses the existing spelling neObvescaj")
  void r00503() {
    contact(ana, acme, "firstName", "A", "tags", arr("neObvescaj"));
    String c = contact(ana, acme, "firstName", "B", "tags", arr("NEOBVESCAJ"));
    assertThat(texts(read(ana, acme, c).path("tags"), null)).containsExactly("neObvescaj");
    assertThat(tags(ana, acme)).hasSize(1);
  }

  @Test
  @DisplayName("R-005-04 boundary 49 Tags plus 1 are accepted, plus 2 refused")
  void r00504() {
    List<String> t = new ArrayList<>();
    for (int i = 0; i < 51; i++) {
      t.add("t" + i);
    }
    String c = contact(ana, acme, "firstName", "M", "tags", t.subList(0, 49));
    save(ana, acme, read(ana, acme, c), "tags", t).is(422);
    edit(ana, acme, c, "tags", t.subList(0, 50));
    assertThat(read(ana, acme, c).path("tags")).hasSize(50);
  }

  @Test
  @DisplayName("R-005-05 combination the same Tag name in Beta and Acme makes two separate Tags")
  void r00505() {
    G beta = group(ana, "Beta");
    contact(ana, beta, "firstName", "B", "tags", arr("gost"));
    contact(ana, acme, "firstName", "A", "tags", arr("gost"));
    String betaTag = tagId(ana, beta, "gost");
    assertThat(tagId(ana, acme, "gost")).isNotEqualTo(betaTag);
    ana.delete(acme.p() + "/tags/" + betaTag).is(404);
  }

  @Test
  @DisplayName("R-005-06 other removing the only use of a Tag keeps it in the Group with 0 Contacts")
  void r00506() {
    String c = contact(ana, acme, "firstName", "M", "tags", arr("osamljen"));
    edit(ana, acme, c, "tags", arr());
    assertThat(tags(ana, acme).get(0).path("contactCount").asInt()).isZero();
  }

  @Test
  @DisplayName("R-005-07 other the facets offer all Tags of the Group")
  void r00507() {
    contact(ana, acme, "firstName", "A", "tags", arr("ena", "dve"));
    contact(ana, acme, "firstName", "B", "tags", arr("tri"));
    assertThat(texts(ana.get(acme.p() + "/facets").is(200).json().path("tags"), null)).containsExactlyInAnyOrder("ena", "dve", "tri");
  }

  @Test
  @DisplayName("R-005-08 other adding a Tag writes TAG_ADDED and raises the version")
  void r00508() {
    String c = contact(ana, acme, "firstName", "M");
    assertThat(edit(ana, acme, c, "tags", arr("gost")).path("version").asInt()).isEqualTo(2);
    assertThat(history(ana, acme, c).get(0).path("action").asText()).isEqualTo("TAG_ADDED");
  }

  @Test
  @DisplayName("R-005-09 role a Viewer sees neObvescaj in the list")
  void r00509() {
    contact(ana, acme, "firstName", "M", "tags", arr("neObvescaj"));
    U viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");
    assertThat(texts(page(viola, acme, "").path("items").get(0).path("tags"), null)).containsExactly("neObvescaj");
  }

  @Test
  @DisplayName("R-005-10 role a Viewer's removal of a Tag is refused")
  void r00510() {
    String c = contact(ana, acme, "firstName", "M", "tags", arr("gost"));
    U viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");
    save(viola, acme, read(viola, acme, c), "tags", arr()).is(403);
    assertThat(read(ana, acme, c).path("tags")).hasSize(1);
  }

  @Test
  @DisplayName("R-006-01 other the Tag list shows 2 and 0 Contacts")
  void r00601() {
    contact(ana, acme, "firstName", "A", "tags", arr("dva"));
    String b = contact(ana, acme, "firstName", "B", "tags", arr("dva", "nic"));
    edit(ana, acme, b, "tags", arr("dva"));
    var counts = tags(ana, acme);
    assertThat(counts.stream().filter(t -> t.path("name").asText().equals("dva")).findFirst().orElseThrow().path("contactCount").asInt()).isEqualTo(2);
    assertThat(counts.stream().filter(t -> t.path("name").asText().equals("nic")).findFirst().orElseThrow().path("contactCount").asInt()).isZero();
  }

  @Test
  @DisplayName("R-006-02 other a rename reaches a Contact in Trash as well")
  void r00602() {
    String c = contact(ana, acme, "firstName", "M", "tags", arr("star"));
    trash(ana, acme, c);
    ana.patch(acme.p() + "/tags/" + tagId(ana, acme, "star"), obj("name", "nov")).is(200);
    ana.post(acme.p() + "/trash/contacts/" + c + "/restore").is(200);
    assertThat(texts(read(ana, acme, c).path("tags"), null)).containsExactly("nov");
  }

  @Test
  @DisplayName("R-006-03 invalid renaming to x/y is refused with the reason")
  void r00603() {
    contact(ana, acme, "firstName", "M", "tags", arr("star"));
    ana.patch(acme.p() + "/tags/" + tagId(ana, acme, "star"), obj("name", "x/y")).is(422);
    assertThat(tags(ana, acme).get(0).path("name").asText()).isEqualTo("star");
  }

  @Test
  @DisplayName("R-006-04 other gost can be renamed to GOST")
  void r00604() {
    contact(ana, acme, "firstName", "M", "tags", arr("gost"));
    ana.patch(acme.p() + "/tags/" + tagId(ana, acme, "gost"), obj("name", "GOST")).is(200);
    assertThat(tags(ana, acme).get(0).path("name").asText()).isEqualTo("GOST");
  }

  @Test
  @DisplayName("R-006-05 combination deleting a Tag on 3 Contacts, one in Trash, deletes no Contact")
  void r00605() {
    contact(ana, acme, "firstName", "A", "tags", arr("x1"));
    contact(ana, acme, "firstName", "B", "tags", arr("x1"));
    String c = contact(ana, acme, "firstName", "C", "tags", arr("x1"));
    trash(ana, acme, c);
    ana.delete(acme.p() + "/tags/" + tagId(ana, acme, "x1")).is(204);
    assertThat(total(ana, acme, "")).isEqualTo(2);
    assertThat(list(ana.get(acme.p() + "/trash/contacts").is(200).json())).hasSize(1);
  }

  @Test
  @DisplayName("R-006-06 other the count shown before deleting is the number of Contacts with the Tag")
  void r00606() {
    for (int i = 0; i < 3; i++) {
      contact(ana, acme, "firstName", "O" + i, "tags", arr("tri"));
    }
    assertThat(tags(ana, acme).get(0).path("contactCount").asInt()).isEqualTo(3);
  }

  @Test
  @DisplayName("R-006-07 other deleting a Tag writes one entry per Contact and keeps the versions")
  void r00607() {
    String a = contact(ana, acme, "firstName", "A", "tags", arr("x1"));
    String b = contact(ana, acme, "firstName", "B", "tags", arr("x1"));
    ana.delete(acme.p() + "/tags/" + tagId(ana, acme, "x1")).is(204);
    assertThat(history(ana, acme, a)).hasSize(2);
    assertThat(history(ana, acme, b)).hasSize(2);
    assertThat(read(ana, acme, a).path("version").asInt()).isEqualTo(1);
  }

  @Test
  @DisplayName("R-006-08 role a Viewer's delete is refused, an Editor's is accepted")
  void r00608() {
    contact(ana, acme, "firstName", "A", "tags", arr("x1"));
    String t = tagId(ana, acme, "x1");
    U viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");
    U boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
    viola.delete(acme.p() + "/tags/" + t).is(403);
    boris.delete(acme.p() + "/tags/" + t).is(204);
  }

  @Test
  @DisplayName("BR-06 boundary company of 255 and e-mail of 254 characters are accepted together")
  void br06() {
    String email = chars(245, 'a') + "@acme.com";
    ana.post(acme.contacts(), obj("company", chars(255, 'c'), "email", email)).is(201);
    ana.post(acme.contacts(), obj("company", chars(256, 'c'), "email", email)).is(422);
    assertThat(total(ana, acme, "")).isEqualTo(1);
  }

  @Test
  @DisplayName("BR-07 other a Tag change sent with an old version is refused")
  void br07() {
    String c = contact(ana, acme, "firstName", "M");
    JsonNode old = read(ana, acme, c);
    edit(ana, acme, c, "tags", arr("a"));
    save(ana, acme, old, "attributes", attrs("k", "v")).is(409, "contact.changed_by_someone_else");
  }
}
