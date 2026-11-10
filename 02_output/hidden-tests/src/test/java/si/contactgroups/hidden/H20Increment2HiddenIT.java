package si.contactgroups.hidden;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Hidden cases of US-020 to US-026. */
class H20Increment2HiddenIT extends HiddenBase {

  U ana;
  G acme;

  @BeforeEach
  void setUp() {
    ana = user("Ana Novak", "ana@acme.com");
    acme = group(ana, "Acme");
  }

  List<String> groupIds(U u) {
    return texts(u.get("/groups").is(200).json(), "id");
  }

  Resp mergeReq(U u, String survivor, List<String> ids, Object fields, List<JsonNode> attributes) {
    var cs = arr();
    for (String id : ids) {
      cs.add(obj("id", id, "version", read(u, acme, id).path("version").asInt()));
    }
    return u.post(acme.p() + "/merges", obj("survivorId", survivor, "contacts", cs, "fields", fields, "attributes", attributes));
  }

  @Test
  @DisplayName("R-020-01 other deleting one of two Groups leaves the other in the Group list")
  void r02001() {
    G beta = group(ana, "Beta");
    ana.post(acme.p() + "/trash", obj("name", "Acme")).is(204);
    assertThat(groupIds(ana)).containsExactly(beta.id());
  }

  @Test
  @DisplayName("R-020-02 invalid an empty typed name deletes nothing")
  void r02002() {
    assertThat(ana.post(acme.p() + "/trash", obj("name", "")).status()).isBetween(400, 499);
    assertThat(groupIds(ana)).contains(acme.id());
  }

  @Test
  @DisplayName("R-020-03 other the Owner too gets not found for members and activity of the Group in Trash")
  void r02003() {
    ana.post(acme.p() + "/trash", obj("name", "Acme")).is(204);
    ana.get(acme.p() + "/members").is(404);
    ana.get(acme.p() + "/activity").is(404);
  }

  @Test
  @DisplayName("R-020-04 other after 10 days in the Group Trash 20 days are left")
  void r02004() {
    ana.post(acme.p() + "/trash", obj("name", "Acme")).is(204);
    clock.advance(Duration.ofDays(10).plusMinutes(1));
    assertThat(list(ana.get("/group-trash").is(200).json()).get(0).path("daysLeft").asInt()).isEqualTo(20);
  }

  @Test
  @DisplayName("R-020-05 boundary after 30 days and 1 hour Tags, Attributes and Versions of the Group are gone")
  void r02005() {
    String c = contact(ana, acme, "firstName", "M", "tags", arr("x"), "attributes", attrs("k", "v"));
    ana.post(acme.p() + "/trash", obj("name", "Acme")).is(204);
    clock.advance(Duration.ofDays(30).plusHours(1));
    awaitTrue(() -> count("contact_group", "id", acme.id()) == 0);
    assertThat(count("attribute", "contact_id", c)).isZero();
    assertThat(count("contact_version", "contact_id", c)).isZero();
    assertThat(count("tag", "group_id", acme.id())).isZero();
  }

  @Test
  @DisplayName("R-020-06 role a Viewer's delete request is refused")
  void r02006() {
    U viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");
    viola.post(acme.p() + "/trash", obj("name", "Acme")).is(403);
    assertThat(groupIds(ana)).contains(acme.id());
  }

  @Test
  @DisplayName("R-020-07 other a restore gives a Viewer access again and keeps a pending Invitation")
  void r02007() {
    U viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");
    ana.post(acme.p() + "/invitations", obj("email", "cene@acme.com", "role", "EDITOR")).is(201);
    ana.post(acme.p() + "/trash", obj("name", "Acme")).is(204);
    ana.post("/group-trash/" + acme.id() + "/restore").is(200);
    assertThat(role(viola, acme)).isEqualTo("VIEWER");
    assertThat(texts(ana.get(acme.p() + "/invitations").is(200).json(), "email")).contains("cene@acme.com");
  }

  @Test
  @DisplayName("R-020-08 combination a Group ' acme ' created meanwhile blocks the restore")
  void r02008() {
    ana.post(acme.p() + "/trash", obj("name", "Acme")).is(204);
    group(ana, " acme ");
    ana.post("/group-trash/" + acme.id() + "/restore").is(409, "group.name_taken");
  }

  @Test
  @DisplayName("R-021-01 other an account with two Memberships is removed from both Groups")
  void r02101() {
    G beta = group(ana, "Beta");
    U boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
    join(beta, ana, boris, "VIEWER");
    boris.delete("/me").is(204);
    assertThat(texts(ana.get(acme.p() + "/members").is(200).json(), "email")).doesNotContain("boris@acme.com");
    assertThat(texts(ana.get(beta.p() + "/members").is(200).json(), "email")).doesNotContain("boris@acme.com");
  }

  @Test
  @DisplayName("R-021-02 combination a Sole Owner of a Group in the Group Trash is refused; it is listed as in Trash")
  void r02102() {
    ana.post(acme.p() + "/trash", obj("name", "Acme")).is(204);
    Resp r = ana.delete("/me").is(409, "account.sole_owner");
    assertThat(r.json().path("groups").get(0).path("inTrash").asBoolean()).isTrue();
    assertThat(accountExists("ana@acme.com")).isTrue();
  }

  @Test
  @DisplayName("R-021-03 other the activity entries of a deleted account keep the name without e-mail")
  void r02103() {
    U boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
    contact(boris, acme, "firstName", "M");
    boris.delete("/me").is(204);
    JsonNode e = activity(ana, acme, "action=CONTACT_CREATED").get(0);
    assertThat(e.path("userName").asText()).isEqualTo("Boris Kos");
    assertThat(e.toString()).doesNotContain("boris@acme.com");
  }

  @Test
  @DisplayName("R-021-04 other a Contact the deleted User edited keeps its latest values")
  void r02104() {
    U boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
    String c = contact(boris, acme, "firstName", "M");
    edit(boris, acme, c, "city", "Bled");
    boris.delete("/me").is(204);
    assertThat(read(ana, acme, c).path("city").asText()).isEqualTo("Bled");
  }

  @Test
  @DisplayName("R-021-05 other a person registering the address again does not see the former Group")
  void r02105() {
    U boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
    boris.delete("/me").is(204);
    U again = user("Boris Kos", "boris@acme.com");
    assertThat(groupIds(again)).isEmpty();
    again.get(acme.contacts()).is(404);
  }

  @Test
  @DisplayName("R-021-06 other without the confirmed request the account stays")
  void r02106() {
    U boris = user("Boris Kos", "boris@acme.com");
    boris.get("/me").is(200);
    assertThat(accountExists("boris@acme.com")).isTrue();
  }

  @Test
  @DisplayName("R-022-01 other five saves give five Versions with user names")
  void r02201() {
    String c = contact(ana, acme, "firstName", "M");
    for (int i = 0; i < 4; i++) {
      edit(ana, acme, c, "city", "K" + i);
    }
    var v = list(ana.get(acme.contact(c) + "/versions").is(200).json());
    assertThat(v).hasSize(5);
    assertThat(v).allMatch(x -> x.path("userName").asText().equals("Ana Novak"));
  }

  @Test
  @DisplayName("R-022-02 other the Versions are numbered 3, 2, 1 newest first")
  void r02202() {
    String c = contact(ana, acme, "firstName", "M");
    edit(ana, acme, c, "city", "A");
    edit(ana, acme, c, "city", "B");
    assertThat(texts(ana.get(acme.contact(c) + "/versions").is(200).json(), "number")).containsExactly("3", "2", "1");
  }

  @Test
  @DisplayName("R-022-03 other Version 2 shows the Attributes it had")
  void r02203() {
    String c = contact(ana, acme, "firstName", "M");
    edit(ana, acme, c, "attributes", attrs("k", "v2"));
    edit(ana, acme, c, "attributes", attrs("k", "v3"));
    JsonNode v = ana.get(acme.contact(c) + "/versions/2").is(200).json();
    assertThat(texts(v.path("content").path("attributes"), "value")).containsExactly("v2");
  }

  @Test
  @DisplayName("R-022-04 combination a changed Attribute and a removed Tag are both listed")
  void r02204() {
    String c = contact(ana, acme, "firstName", "M", "tags", arr("t"), "attributes", attrs("k", "a"));
    edit(ana, acme, c, "tags", arr(), "attributes", attrs("k", "b"));
    var d = list(ana.get(acme.contact(c) + "/versions/1/compare").is(200).json().path("differences"));
    assertThat(texts(JSON.valueToTree(d), "kind")).containsExactlyInAnyOrder("tag", "attribute");
  }

  @Test
  @DisplayName("R-022-05 invalid a POST to a Version is refused and the Version stays")
  void r02205() {
    String c = contact(ana, acme, "firstName", "M");
    JsonNode before = ana.get(acme.contact(c) + "/versions/1").is(200).json();
    assertThat(ana.post(acme.contact(c) + "/versions/1", obj("content", obj())).status()).isBetween(400, 499);
    assertThat(ana.get(acme.contact(c) + "/versions/1").is(200).json()).isEqualTo(before);
  }

  @Test
  @DisplayName("R-022-06 role a Viewer's comparison is refused")
  void r02206() {
    String c = contact(ana, acme, "firstName", "M");
    U viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");
    viola.get(acme.contact(c) + "/versions/1/compare").is(403);
  }

  @Test
  @DisplayName("R-022-07 other the clean-up after 30 days deletes the Versions")
  void r02207() {
    String c = contact(ana, acme, "firstName", "M");
    edit(ana, acme, c, "city", "A");
    trash(ana, acme, c);
    clock.advance(Duration.ofDays(31));
    awaitTrue(() -> count("contact_version", "contact_id", c) == 0);
  }

  @Test
  @DisplayName("R-023-01 other restoring v2 of 4 gives v5 with the content of v2")
  void r02301() {
    String c = contact(ana, acme, "firstName", "M", "city", "V1");
    edit(ana, acme, c, "city", "V2");
    edit(ana, acme, c, "city", "V3");
    edit(ana, acme, c, "city", "V4");
    JsonNode r = ana.post(acme.contact(c) + "/versions/2/restore", obj("version", 4)).is(200).json();
    assertThat(r.path("version").asInt()).isEqualTo(5);
    assertThat(r.path("city").asText()).isEqualTo("V2");
    assertThat(list(ana.get(acme.contact(c) + "/versions").is(200).json())).hasSize(5);
  }

  @Test
  @DisplayName("R-023-02 other the restore entry names version 2 and the changed city")
  void r02302() {
    String c = contact(ana, acme, "firstName", "M", "city", "Prvo");
    edit(ana, acme, c, "city", "Drugo");
    edit(ana, acme, c, "city", "Tretje");
    ana.post(acme.contact(c) + "/versions/2/restore", obj("version", 3)).is(200);
    JsonNode e = history(ana, acme, c).get(0);
    assertThat(e.path("action").asText()).isEqualTo("VERSION_RESTORED");
    assertThat(e.path("details").toString()).contains("2").contains("Tretje").contains("Drugo");
  }

  @Test
  @DisplayName("R-023-03 invalid a Version with a phone of letters (an earlier rule) is not restored")
  void r02303() throws Exception {
    String c = contact(ana, acme, "firstName", "M", "phone", "040 111 222");
    edit(ana, acme, c, "phone", "040 333 444");
    try (Connection con = owner();
        PreparedStatement s = con.prepareStatement(
            "update contact_version set snapshot = replace(snapshot::text, '\"040 111 222\"', '\"telefon\"')::jsonb where contact_id = ?::uuid and number = 1")) {
      s.setString(1, c);
      assertThat(s.executeUpdate()).isEqualTo(1);
    }
    ana.post(acme.contact(c) + "/versions/1/restore", obj("version", 2)).is(422);
    assertThat(read(ana, acme, c).path("phone").asText()).isEqualTo("040 333 444");
  }

  @Test
  @DisplayName("R-023-04 other a restore sent with version 1 when the Contact is at 2 is refused")
  void r02304() {
    String c = contact(ana, acme, "firstName", "M");
    edit(ana, acme, c, "city", "A");
    ana.post(acme.contact(c) + "/versions/1/restore", obj("version", 1)).is(409, "contact.changed_by_someone_else");
  }

  @Test
  @DisplayName("R-023-05 combination a deleted Tag is created again and an existing one is reused")
  void r02305() {
    String c = contact(ana, acme, "firstName", "M", "tags", arr("izbrisan", "obstojec"));
    edit(ana, acme, c, "tags", arr("obstojec"));
    ana.delete(acme.p() + "/tags/" + tagId(ana, acme, "izbrisan")).is(204);
    ana.post(acme.contact(c) + "/versions/1/restore", obj("version", 2)).is(200);
    assertThat(texts(read(ana, acme, c).path("tags"), null)).containsExactlyInAnyOrder("izbrisan", "obstojec");
    assertThat(tags(ana, acme)).hasSize(2);
  }

  @Test
  @DisplayName("R-023-06 role a Viewer's restore of v1 is refused")
  void r02306() {
    String c = contact(ana, acme, "firstName", "M");
    edit(ana, acme, c, "city", "A");
    U viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");
    viola.post(acme.contact(c) + "/versions/1/restore", obj("version", 2)).is(403);
  }

  List<JsonNode> sets() {
    return list(ana.get(acme.p() + "/duplicates").is(200).json());
  }

  @Test
  @DisplayName("R-024-01 other only Contacts not in Trash are compared")
  void r02401() {
    contact(ana, acme, "firstName", "A", "email", "x@acme.com");
    String b = contact(ana, acme, "firstName", "B", "email", "x@acme.com");
    contact(ana, acme, "firstName", "C", "email", "x@acme.com");
    trash(ana, acme, b);
    assertThat(texts(sets().get(0).path("contacts"), "id")).hasSize(2).doesNotContain(b);
  }

  @Test
  @DisplayName("R-024-02 other two e-mails differing only in case are certain")
  void r02402() {
    contact(ana, acme, "firstName", "A", "email", "Mix@Acme.com");
    contact(ana, acme, "firstName", "B", "email", "mix@acme.COM");
    assertThat(sets().get(0).path("mark").asText()).isEqualTo("certain");
  }

  @Test
  @DisplayName("R-024-03 combination Žiga Čeh of ČEH d.o.o. and ziga ceh of Ceh d.o.o. are probable")
  void r02403() {
    contact(ana, acme, "firstName", "Žiga", "lastName", "Čeh", "company", "ČEH d.o.o.");
    contact(ana, acme, "firstName", "ziga", "lastName", "ceh", "company", "Ceh d.o.o.");
    assertThat(sets()).hasSize(1);
    assertThat(sets().get(0).path("mark").asText()).isEqualTo("probable");
  }

  @Test
  @DisplayName("R-024-04 other Contacts with only the same company are no set")
  void r02404() {
    contact(ana, acme, "company", "Enako d.o.o.");
    contact(ana, acme, "company", "Enako d.o.o.");
    assertThat(sets()).isEmpty();
  }

  @Test
  @DisplayName("R-024-05 other a probable set carries its mark, a reason and its Contacts")
  void r02405() {
    contact(ana, acme, "firstName", "Ana", "lastName", "Kos", "phone", "041 000 000");
    contact(ana, acme, "firstName", "Ana", "lastName", "Kos", "phone", "041000000");
    JsonNode s = sets().get(0);
    assertThat(s.path("reasonCode").asText()).isNotBlank();
    assertThat(s.path("contacts")).hasSize(2);
  }

  @Test
  @DisplayName("R-024-06 other a search writes no History entry")
  void r02406() {
    contact(ana, acme, "firstName", "A", "email", "x@acme.com");
    contact(ana, acme, "firstName", "B", "email", "x@acme.com");
    int n = activity(ana, acme, "").size();
    sets();
    assertThat(activity(ana, acme, "")).hasSize(n);
  }

  @Test
  @DisplayName("R-024-07 other a set of Beta is not listed in Acme and the other way round")
  void r02407() {
    G beta = group(ana, "Beta");
    contact(ana, beta, "firstName", "A", "email", "b@acme.com");
    contact(ana, beta, "firstName", "B", "email", "b@acme.com");
    contact(ana, acme, "firstName", "C", "email", "b@acme.com");
    assertThat(sets()).isEmpty();
    assertThat(list(ana.get(beta.p() + "/duplicates").is(200).json())).hasSize(1);
  }

  @Test
  @DisplayName("R-024-08 combination A in a certain set with B and a probable match with C shows A only once")
  void r02408() {
    String a = contact(ana, acme, "firstName", "Eva", "lastName", "Zor", "phone", "031 111 111", "email", "eva@acme.com");
    contact(ana, acme, "firstName", "Evica", "email", "EVA@acme.com");
    contact(ana, acme, "firstName", "eva", "lastName", "zor", "phone", "031111111");
    long withA = sets().stream().filter(s -> texts(s.path("contacts"), "id").contains(a)).count();
    assertThat(withA).isEqualTo(1);
  }

  @Test
  @DisplayName("R-024-09 role a Viewer's search is refused, an Editor's answered")
  void r02409() {
    U viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");
    U boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
    viola.get(acme.p() + "/duplicates").is(403);
    boris.get(acme.p() + "/duplicates").is(200);
  }

  @Test
  @DisplayName("R-024-10 other an empty Group has no Duplicate set")
  void r02410() {
    assertThat(sets()).isEmpty();
  }

  @Test
  @DisplayName("R-025-01 other a certain set marked 'not a duplicate' is hidden")
  void r02501() {
    String a = contact(ana, acme, "firstName", "A", "email", "x@acme.com");
    String b = contact(ana, acme, "firstName", "B", "email", "x@acme.com");
    ana.post(acme.p() + "/duplicates/dismissals", obj("contactIds", arr(b, a))).is(204);
    assertThat(sets()).isEmpty();
  }

  @Test
  @DisplayName("R-025-02 other a dismissal stays after unrelated Contacts are added")
  void r02502() {
    String a = contact(ana, acme, "firstName", "A", "email", "x@acme.com");
    String b = contact(ana, acme, "firstName", "B", "email", "x@acme.com");
    ana.post(acme.p() + "/duplicates/dismissals", obj("contactIds", arr(a, b))).is(204);
    contact(ana, acme, "firstName", "C", "email", "c@acme.com");
    assertThat(sets()).isEmpty();
  }

  @Test
  @DisplayName("R-025-03 other a fourth Contact matching a dismissed set of 3 makes the set shown")
  void r02503() {
    String a = contact(ana, acme, "firstName", "A", "email", "x@acme.com");
    String b = contact(ana, acme, "firstName", "B", "email", "x@acme.com");
    String c = contact(ana, acme, "firstName", "C", "email", "x@acme.com");
    ana.post(acme.p() + "/duplicates/dismissals", obj("contactIds", arr(a, b, c))).is(204);
    contact(ana, acme, "firstName", "D", "email", "x@acme.com");
    assertThat(sets().get(0).path("contacts")).hasSize(4);
  }

  @Test
  @DisplayName("R-025-04 role a Viewer's dismissal is refused and the set stays")
  void r02504() {
    String a = contact(ana, acme, "firstName", "A", "email", "x@acme.com");
    String b = contact(ana, acme, "firstName", "B", "email", "x@acme.com");
    U viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");
    viola.post(acme.p() + "/duplicates/dismissals", obj("contactIds", arr(a, b))).is(403);
    assertThat(sets()).hasSize(1);
  }

  @Test
  @DisplayName("R-025-05 other a dismissal changes no updatedAt")
  void r02505() {
    String a = contact(ana, acme, "firstName", "A", "email", "x@acme.com");
    String b = contact(ana, acme, "firstName", "B", "email", "x@acme.com");
    String before = read(ana, acme, a).path("updatedAt").asText();
    ana.post(acme.p() + "/duplicates/dismissals", obj("contactIds", arr(a, b))).is(204);
    assertThat(read(ana, acme, a).path("updatedAt").asText()).isEqualTo(before);
  }

  @Test
  @DisplayName("R-026-01 other a Duplicate set offers the Contacts the merge needs (id and version)")
  void r02601() {
    contact(ana, acme, "firstName", "A", "email", "x@acme.com");
    contact(ana, acme, "firstName", "B", "email", "x@acme.com");
    assertThat(list(sets().get(0).path("contacts"))).allMatch(c -> c.has("id") && c.has("version"));
  }

  @Test
  @DisplayName("R-026-02 other a merge with a value of the newer Contact keeps that value")
  void r02602() {
    String a = contact(ana, acme, "firstName", "A", "email", "x@acme.com");
    String b = contact(ana, acme, "firstName", "B", "email", "x@acme.com", "city", "Novo");
    mergeReq(ana, a, List.of(a, b), obj("firstName", "A", "email", "x@acme.com", "city", "Novo"), List.of()).is(200);
    assertThat(read(ana, acme, a).path("city").asText()).isEqualTo("Novo");
  }

  @Test
  @DisplayName("R-026-03 other three Contacts' Tags are combined")
  void r02603() {
    String a = contact(ana, acme, "firstName", "A", "email", "x@acme.com", "tags", arr("t1"));
    String b = contact(ana, acme, "firstName", "B", "email", "x@acme.com", "tags", arr("t2", "t1"));
    String c = contact(ana, acme, "firstName", "C", "email", "x@acme.com", "tags", arr("t3"));
    mergeReq(ana, a, List.of(a, b, c), obj("firstName", "A", "email", "x@acme.com"), List.of()).is(200);
    assertThat(texts(read(ana, acme, a).path("tags"), null)).containsExactlyInAnyOrder("t1", "t2", "t3");
  }

  @Test
  @DisplayName("R-026-04 combination the chosen value of a clashing key is kept and other keys are combined")
  void r02604() {
    String a = contact(ana, acme, "firstName", "A", "email", "x@acme.com", "attributes", attrs("jezik", "en", "hobi", "šah"));
    String b = contact(ana, acme, "firstName", "B", "email", "x@acme.com", "attributes", attrs("jezik", "de", "mesto", "Kranj"));
    mergeReq(ana, a, List.of(a, b), obj("firstName", "A", "email", "x@acme.com"), attrs("jezik", "de", "hobi", "šah", "mesto", "Kranj")).is(200);
    JsonNode at = read(ana, acme, a).path("attributes");
    assertThat(at).hasSize(3);
    assertThat(list(at).stream().filter(x -> x.path("key").asText().equals("jezik")).findFirst().orElseThrow().path("value").asText()).isEqualTo("de");
  }

  @Test
  @DisplayName("R-026-05 other the Survivor gets a new Version")
  void r02605() {
    String a = contact(ana, acme, "firstName", "A", "email", "x@acme.com");
    String b = contact(ana, acme, "firstName", "B", "email", "x@acme.com");
    mergeReq(ana, a, List.of(a, b), obj("firstName", "B", "email", "x@acme.com"), List.of()).is(200);
    assertThat(list(ana.get(acme.contact(a) + "/versions").is(200).json())).hasSize(2);
  }

  @Test
  @DisplayName("R-026-06 invalid a chosen value that no merged Contact has is refused")
  void r02606() {
    String a = contact(ana, acme, "firstName", "A", "email", "x@acme.com");
    String b = contact(ana, acme, "firstName", "B", "email", "x@acme.com");
    mergeReq(ana, a, List.of(a, b), obj("firstName", "Izmišljen", "email", "x@acme.com"), List.of()).is(422);
    assertThat(read(ana, acme, a).path("firstName").asText()).isEqualTo("A");
  }

  @Test
  @DisplayName("R-026-07 other merging three Contacts writes one merge entry")
  void r02607() {
    String a = contact(ana, acme, "firstName", "A", "email", "x@acme.com");
    String b = contact(ana, acme, "firstName", "B", "email", "x@acme.com");
    String c = contact(ana, acme, "firstName", "C", "email", "x@acme.com");
    mergeReq(ana, a, List.of(a, b, c), obj("firstName", "A", "email", "x@acme.com"), List.of()).is(200);
    String d = activity(ana, acme, "action=CONTACTS_MERGED").get(0).path("details").toString();
    assertThat(d).contains(b).contains(c);
  }

  @Test
  @DisplayName("R-026-08 other a Survivor changed after the screen opened refuses the merge")
  void r02608() {
    String a = contact(ana, acme, "firstName", "A", "email", "x@acme.com");
    String b = contact(ana, acme, "firstName", "B", "email", "x@acme.com");
    var body = obj("survivorId", a, "contacts", arr(obj("id", a, "version", 1), obj("id", b, "version", 1)),
        "fields", obj("firstName", "A", "email", "x@acme.com"), "attributes", arr());
    edit(ana, acme, a, "city", "Bled");
    ana.post(acme.p() + "/merges", body).is(409, "contact.changed_by_someone_else");
    read(ana, acme, b);
  }

  @Test
  @DisplayName("R-026-09 invalid a Contact id that does not exist refuses the merge")
  void r02609() {
    String a = contact(ana, acme, "firstName", "A", "email", "x@acme.com");
    var body = obj("survivorId", a, "contacts", arr(obj("id", a, "version", 1), obj("id", uuid(), "version", 1)),
        "fields", obj("firstName", "A", "email", "x@acme.com"), "attributes", arr());
    assertThat(ana.post(acme.p() + "/merges", body).status()).isIn(404, 422);
    assertThat(read(ana, acme, a).path("version").asInt()).isEqualTo(1);
  }

  @Test
  @DisplayName("R-026-10 other a Duplicate search alone merges nothing")
  void r02610() {
    contact(ana, acme, "firstName", "A", "email", "x@acme.com");
    contact(ana, acme, "firstName", "B", "email", "x@acme.com");
    sets();
    sets();
    assertThat(total(ana, acme, "")).isEqualTo(2);
  }

  @Test
  @DisplayName("R-026-11 boundary a merged-away Contact can be restored after 29 days")
  void r02611() {
    String a = contact(ana, acme, "firstName", "A", "email", "x@acme.com");
    String b = contact(ana, acme, "firstName", "B", "email", "x@acme.com");
    mergeReq(ana, a, List.of(a, b), obj("firstName", "A", "email", "x@acme.com"), List.of()).is(200);
    clock.advance(Duration.ofDays(29));
    ana.post(acme.p() + "/trash/contacts/" + b + "/restore").is(200);
  }

  @Test
  @DisplayName("R-026-12 role a Viewer's merge is refused, an Editor's accepted")
  void r02612() {
    String a = contact(ana, acme, "firstName", "A", "email", "x@acme.com");
    String b = contact(ana, acme, "firstName", "B", "email", "x@acme.com");
    U viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");
    U boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
    mergeReq(viola, a, List.of(a, b), obj("firstName", "A", "email", "x@acme.com"), List.of()).is(403);
    mergeReq(boris, a, List.of(a, b), obj("firstName", "A", "email", "x@acme.com"), List.of()).is(200);
  }
}
