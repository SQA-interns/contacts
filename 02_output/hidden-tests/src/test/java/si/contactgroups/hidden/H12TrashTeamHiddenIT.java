package si.contactgroups.hidden;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Hidden cases of US-012, US-013, US-014, US-015, BR-01, BR-02. */
class H12TrashTeamHiddenIT extends HiddenBase {

  U ana;
  G acme;

  @BeforeEach
  void setUp() {
    ana = user("Ana Novak", "ana@acme.com");
    acme = group(ana, "Acme");
  }

  List<JsonNode> trashList(U u) {
    return list(u.get(acme.p() + "/trash/contacts").is(200).json());
  }

  String restore(String c) {
    return acme.p() + "/trash/contacts/" + c + "/restore";
  }

  @Test
  @DisplayName("R-012-01 other without the confirmed delete request the Contact stays listed")
  void r01201() {
    String c = contact(ana, acme, "firstName", "M");
    read(ana, acme, c);
    assertThat(ids(ana, acme, "")).containsExactly(c);
  }

  @Test
  @DisplayName("R-012-02 combination a Contact in Trash is hidden from the Tag filter and the facets of its company")
  void r01202() {
    String c = contact(ana, acme, "firstName", "M", "company", "Sama d.o.o.", "tags", arr("x"));
    trash(ana, acme, c);
    assertThat(total(ana, acme, "tags=x&tagMode=all")).isZero();
    assertThat(texts(ana.get(acme.p() + "/facets").is(200).json().path("companies"), null)).doesNotContain("Sama d.o.o.");
  }

  @Test
  @DisplayName("R-012-03 other after 10 days in Trash 20 days are left")
  void r01203() {
    trash(ana, acme, contact(ana, acme, "firstName", "M"));
    clock.advance(Duration.ofDays(10).plusMinutes(1));
    assertThat(trashList(ana).get(0).path("daysLeft").asInt()).isEqualTo(20);
  }

  @Test
  @DisplayName("R-012-04 other a restore keeps version 3 and the notes")
  void r01204() {
    String c = contact(ana, acme, "firstName", "M", "notes", "pomembno");
    edit(ana, acme, c, "city", "A");
    edit(ana, acme, c, "city", "B");
    trash(ana, acme, c);
    var back = ana.post(restore(c)).is(200).json();
    assertThat(back.path("version").asInt()).isEqualTo(3);
    assertThat(read(ana, acme, c).path("notes").asText()).isEqualTo("pomembno");
  }

  @Test
  @DisplayName("R-012-05 boundary a restore after 31 days is not found")
  void r01205() {
    String c = contact(ana, acme, "firstName", "M");
    trash(ana, acme, c);
    clock.advance(Duration.ofDays(31));
    ana.post(restore(c)).is(404);
  }

  @Test
  @DisplayName("R-012-06 invalid editing a Contact 2 days in Trash is refused")
  void r01206() {
    String c = contact(ana, acme, "firstName", "M");
    JsonNode before = read(ana, acme, c);
    trash(ana, acme, c);
    clock.advance(Duration.ofDays(2));
    assertThat(save(ana, acme, before, "firstName", "X").status()).isBetween(400, 499);
    ana.post(restore(c)).is(200);
    assertThat(read(ana, acme, c).path("firstName").asText()).isEqualTo("M");
  }

  @Test
  @DisplayName("R-012-07 other delete and restore are entries by the acting Editor")
  void r01207() {
    U boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
    String c = contact(ana, acme, "firstName", "M");
    trash(boris, acme, c);
    boris.post(restore(c)).is(200);
    var a = activity(ana, acme, "userId=" + boris.userId());
    assertThat(actions(a)).contains("CONTACT_DELETED", "CONTACT_RESTORED");
  }

  @Test
  @DisplayName("R-012-08 role an Editor may open the Trash view, a Viewer may not")
  void r01208() {
    U boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
    U viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");
    boris.get(acme.p() + "/trash/contacts").is(200);
    viola.get(acme.p() + "/trash/contacts").is(403);
  }

  @Test
  @DisplayName("R-012-09 other a third delete request still changes nothing")
  void r01209() {
    String c = contact(ana, acme, "firstName", "M");
    for (int i = 0; i < 3; i++) {
      assertThat(ana.post(acme.contact(c) + "/trash").status()).isBetween(200, 299);
    }
    assertThat(actions(activity(ana, acme, "action=CONTACT_DELETED"))).hasSize(1);
  }

  @Test
  @DisplayName("R-013-01 other a Contact moved to Trash a minute ago can be deleted forever")
  void r01301() {
    String c = contact(ana, acme, "firstName", "M");
    trash(ana, acme, c);
    ana.delete(acme.p() + "/trash/contacts/" + c).is(204);
    assertThat(trashList(ana)).isEmpty();
  }

  @Test
  @DisplayName("R-013-02 other without the delete-forever request the Contact stays in Trash")
  void r01302() {
    String c = contact(ana, acme, "firstName", "M");
    trash(ana, acme, c);
    assertThat(texts(ana.get(acme.p() + "/trash/contacts").is(200).json(), "id")).containsExactly(c);
  }

  @Test
  @DisplayName("R-013-03 role a Viewer's delete forever is refused")
  void r01303() {
    String c = contact(ana, acme, "firstName", "M");
    trash(ana, acme, c);
    U viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");
    viola.delete(acme.p() + "/trash/contacts/" + c).is(403);
    assertThat(trashList(ana)).hasSize(1);
  }

  @Test
  @DisplayName("R-013-04 boundary after 30 days and 23 hours the clean-up has removed the Contact")
  void r01304() {
    String c = contact(ana, acme, "firstName", "M");
    trash(ana, acme, c);
    clock.advance(Duration.ofDays(30).plusHours(23));
    awaitTrue(() -> count("contact", "id", c) == 0);
  }

  @Test
  @DisplayName("R-013-05 other a Contact deleted forever is found by no search and no export")
  void r01305() {
    String c = contact(ana, acme, "firstName", "Izginjen");
    trash(ana, acme, c);
    ana.delete(acme.p() + "/trash/contacts/" + c).is(204);
    assertThat(total(ana, acme, "q=izginjen")).isZero();
    assertThat(xlsx(export(ana, acme, "en", obj("contactIds", arr(c))).is(200).bytes()).rows()).isEmpty();
  }

  @Test
  @DisplayName("R-013-06 other the clean-up writes a 'deleted forever' entry without personal data")
  void r01306() {
    String c = contact(ana, acme, "firstName", "Skrivna", "lastName", "Oseba", "email", "skrivna@x.si");
    trash(ana, acme, c);
    clock.advance(Duration.ofDays(30).plusHours(1));
    awaitTrue(() -> !activity(ana, acme, "action=CONTACT_DELETED_FOREVER").isEmpty());
    String e = activity(ana, acme, "action=CONTACT_DELETED_FOREVER").get(0).toString();
    assertThat(e).doesNotContain("Skrivna").doesNotContain("skrivna@x.si");
  }

  @Test
  @DisplayName("R-013-07 other the Tags stay after the clean-up removed the Contact")
  void r01307() {
    String c = contact(ana, acme, "firstName", "M", "tags", arr("ostane"));
    trash(ana, acme, c);
    clock.advance(Duration.ofDays(31));
    awaitTrue(() -> count("contact", "id", c) == 0);
    assertThat(texts(ana.get(acme.p() + "/tags").is(200).json(), "name")).containsExactly("ostane");
  }

  @Test
  @DisplayName("R-013-08 combination an Attribute entry of a Contact deleted forever reads removed")
  void r01308() {
    String c = contact(ana, acme, "firstName", "M", "attributes", attrs("geslo", "zaupno123"));
    trash(ana, acme, c);
    ana.delete(acme.p() + "/trash/contacts/" + c).is(204);
    String all = activity(ana, acme, "").toString();
    assertThat(all).doesNotContain("zaupno123");
    assertThat(all).contains("removed");
  }

  @Test
  @DisplayName("R-014-01 role an Invitation may offer the Role Owner")
  void r01401() {
    var inv = ana.post(acme.p() + "/invitations", obj("email", "cene@acme.com", "role", "OWNER")).is(201).json();
    assertThat(inv.path("role").asText()).isEqualTo("OWNER");
    U cene = user("Cene Zupan", "cene@acme.com");
    assertThat(role(cene, acme)).isEqualTo("OWNER");
  }

  @Test
  @DisplayName("R-014-02 invalid the address 'boris acme.com' creates no Invitation")
  void r01402() {
    ana.post(acme.p() + "/invitations", obj("email", "boris acme.com", "role", "EDITOR")).is(422);
    assertThat(list(ana.get(acme.p() + "/invitations").is(200).json())).isEmpty();
  }

  @Test
  @DisplayName("R-014-03 boundary with 20 pending one cancelled makes room for a new one")
  void r01403() {
    String first = null;
    for (int i = 0; i < 20; i++) {
      String id = ana.post(acme.p() + "/invitations", obj("email", "o" + i + "@acme.com", "role", "VIEWER")).is(201).id();
      first = first == null ? id : first;
    }
    ana.post(acme.p() + "/invitations/" + first + "/cancel").is(200);
    ana.post(acme.p() + "/invitations", obj("email", "nova@acme.com", "role", "VIEWER")).is(201);
  }

  @Test
  @DisplayName("R-014-04 combination a pending Invitation in Beta does not block one in Acme")
  void r01404() {
    G beta = group(ana, "Beta");
    ana.post(beta.p() + "/invitations", obj("email", "boris@acme.com", "role", "VIEWER")).is(201);
    ana.post(acme.p() + "/invitations", obj("email", "boris@acme.com", "role", "VIEWER")).is(201);
  }

  @Test
  @DisplayName("R-014-05 other an account BORIS@ACME.COM accepts the Invitation for boris@acme.com")
  void r01405() {
    ana.post(acme.p() + "/invitations", obj("email", "boris@acme.com", "role", "VIEWER")).is(201);
    U boris = account("Boris Kos", "BORIS@ACME.COM", true);
    assertThat(role(boris, acme)).isEqualTo("VIEWER");
  }

  @Test
  @DisplayName("R-014-06 other an unverified invitee gets no Membership even after calling twice")
  void r01406() {
    ana.post(acme.p() + "/invitations", obj("email", "boris@acme.com", "role", "VIEWER")).is(201);
    U boris = account("Boris Kos", "boris@acme.com", false);
    boris.get("/groups").is(403);
    boris.get("/groups").is(403);
    assertThat(texts(ana.get(acme.p() + "/members").is(200).json(), "email")).doesNotContain("boris@acme.com");
  }

  @Test
  @DisplayName("R-014-07 other a cancelled Invitation leaves the pending list")
  void r01407() {
    String id = ana.post(acme.p() + "/invitations", obj("email", "cene@acme.com", "role", "VIEWER")).is(201).id();
    ana.post(acme.p() + "/invitations/" + id + "/cancel").is(200);
    assertThat(list(ana.get(acme.p() + "/invitations").is(200).json())).isEmpty();
  }

  @Test
  @DisplayName("R-014-08 boundary an Invitation 14 days and 1 minute old gives no access")
  void r01408() {
    clock.setNow(Instant.now().minus(Duration.ofDays(14).plusMinutes(1)));
    ana.post(acme.p() + "/invitations", obj("email", "boris@acme.com", "role", "VIEWER")).is(201);
    clock.reset();
    assertThat(role(account("Boris Kos", "boris@acme.com", true), acme)).isNull();
  }

  @Test
  @DisplayName("R-014-09 other two pending Invitations are listed with their Roles and expiry dates")
  void r01409() {
    ana.post(acme.p() + "/invitations", obj("email", "a@acme.com", "role", "VIEWER")).is(201);
    ana.post(acme.p() + "/invitations", obj("email", "b@acme.com", "role", "OWNER")).is(201);
    var l = list(ana.get(acme.p() + "/invitations").is(200).json());
    assertThat(texts(JSON.valueToTree(l), "role")).containsExactlyInAnyOrder("VIEWER", "OWNER");
    assertThat(l).allMatch(i -> !i.path("expiresAt").asText().isBlank());
  }

  @Test
  @DisplayName("R-014-10 role a Viewer's cancel is refused")
  void r01410() {
    String id = ana.post(acme.p() + "/invitations", obj("email", "cene@acme.com", "role", "VIEWER")).is(201).id();
    U viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");
    viola.post(acme.p() + "/invitations/" + id + "/cancel").is(403);
    assertThat(list(ana.get(acme.p() + "/invitations").is(200).json())).hasSize(1);
  }

  @Test
  @DisplayName("R-014-11 other a cancellation is recorded")
  void r01411() {
    String id = ana.post(acme.p() + "/invitations", obj("email", "cene@acme.com", "role", "VIEWER")).is(201).id();
    ana.post(acme.p() + "/invitations/" + id + "/cancel").is(200);
    assertThat(actions(activity(ana, acme, ""))).contains("INVITATION_CREATED", "INVITATION_CANCELLED");
  }

  @Test
  @DisplayName("R-014-12 other neither the Invitation nor the joining sends an e-mail")
  void r01412() {
    ana.post(acme.p() + "/invitations", obj("email", "cene@acme.com", "role", "VIEWER")).is(201);
    user("Cene Zupan", "cene@acme.com").get("/groups").is(200);
    JsonNode codes = emulator("GET", "/emulator/v1/projects/" + PROJECT + "/oobCodes", null, false);
    assertThat(texts(codes.path("oobCodes"), "email")).doesNotContain("cene@acme.com");
  }

  @Test
  @DisplayName("R-015-01 role an Editor sees the Member list")
  void r01501() {
    U boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
    assertThat(list(boris.get(acme.p() + "/members").is(200).json())).hasSize(2);
  }

  @Test
  @DisplayName("R-015-02 other a Viewer promoted to Editor can create a Contact at once")
  void r01502() {
    U viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");
    viola.post(acme.contacts(), obj("firstName", "X")).is(403);
    ana.patch(acme.p() + "/members/" + memberId(ana, acme, "viola@acme.com"), obj("role", "EDITOR")).is(200);
    viola.post(acme.contacts(), obj("firstName", "X")).is(201);
  }

  @Test
  @DisplayName("R-015-03 other a removed Member gets not found for the Member list")
  void r01503() {
    U boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
    ana.delete(acme.p() + "/members/" + memberId(ana, acme, "boris@acme.com")).is(204);
    boris.get(acme.p() + "/members").is(404);
  }

  @Test
  @DisplayName("R-015-04 combination one of two Owners removes the other, then cannot remove herself")
  void r01504() {
    member(acme, ana, "Cene Zupan", "cene@acme.com", "OWNER");
    ana.delete(acme.p() + "/members/" + memberId(ana, acme, "cene@acme.com")).is(204);
    ana.delete(acme.p() + "/members/" + memberId(ana, acme, "ana@acme.com")).is(409, "group.last_owner");
  }

  @Test
  @DisplayName("R-015-05 role a Viewer's removal of a Member is refused")
  void r01505() {
    U viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");
    member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
    viola.delete(acme.p() + "/members/" + memberId(ana, acme, "boris@acme.com")).is(403);
    assertThat(list(ana.get(acme.p() + "/members").is(200).json())).hasSize(3);
  }

  @Test
  @DisplayName("R-015-06 other a promotion to Owner is recorded with both Roles")
  void r01506() {
    member(acme, ana, "Boris Kos", "boris@acme.com", "VIEWER");
    ana.patch(acme.p() + "/members/" + memberId(ana, acme, "boris@acme.com"), obj("role", "OWNER")).is(200);
    String d = activity(ana, acme, "action=ROLE_CHANGED").get(0).path("details").toString();
    assertThat(d).contains("VIEWER").contains("OWNER");
  }

  @Test
  @DisplayName("R-015-07 other the edit entries of a removed Member keep the name")
  void r01507() {
    U boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
    String c = contact(ana, acme, "firstName", "M");
    edit(boris, acme, c, "city", "Bled");
    ana.delete(acme.p() + "/members/" + memberId(ana, acme, "boris@acme.com")).is(204);
    assertThat(history(ana, acme, c).get(0).path("userName").asText()).isEqualTo("Boris Kos");
  }

  @Test
  @DisplayName("R-015-08 other a person who never joined gets not found for Contacts and activity")
  void r01508() {
    U x = user("Tuja Oseba", "tuja@x.si");
    x.get(acme.contacts()).is(404);
    x.get(acme.p() + "/activity").is(404);
  }

  @Test
  @DisplayName("BR-01 other the same endpoint answers 404 to a non-Member and 403 to a Viewer")
  void br01() {
    U viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");
    U x = user("Tuja Oseba", "tuja@x.si");
    viola.get(acme.p() + "/activity").is(403);
    x.get(acme.p() + "/activity").is(404);
  }

  @Test
  @DisplayName("BR-02 role an Editor may export but neither invite nor delete forever nor delete the Group")
  void br02() {
    U boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
    String c = contact(ana, acme, "firstName", "M");
    export(boris, acme, "en", obj("contactIds", arr(c))).is(200);
    boris.post(acme.p() + "/invitations", obj("email", "x@acme.com", "role", "VIEWER")).is(403);
    trash(ana, acme, c);
    boris.delete(acme.p() + "/trash/contacts/" + c).is(403);
    boris.post(acme.p() + "/trash", obj("name", "Acme")).is(403);
  }
}
