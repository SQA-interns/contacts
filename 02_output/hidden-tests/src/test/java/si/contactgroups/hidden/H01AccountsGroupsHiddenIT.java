package si.contactgroups.hidden;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Hidden cases of US-001 (backend side) and US-002. */
class H01AccountsGroupsHiddenIT extends HiddenBase {

  @Test
  @DisplayName("R-001-02 other Google profile Mojca Kovač becomes the display name")
  void r00102() {
    String t = googleToken("mojca@kovac.si", "Mojca Kovač");
    assertThat(send("GET", "/me", t, null, Map.of()).is(200).json().path("displayName").asText()).isEqualTo("Mojca Kovač");
  }

  @Test
  @DisplayName("R-001-03 other a verified Google user gets the Group list")
  void r00103() {
    String t = googleToken("mojca@kovac.si", "Mojca Kovač");
    send("GET", "/groups", t, null, Map.of()).is(200);
  }

  @Test
  @DisplayName("R-001-04 invalid an unverified User cannot create a Group or read one")
  void r00104() {
    U ana = user("Ana Novak", "ana@acme.com");
    G acme = group(ana, "Acme");
    U neza = account("Neža Kos", "neza@acme.com", false);
    neza.post("/groups", obj("name", "Moja")).is(403, "auth.email_not_verified");
    neza.get(acme.contacts()).is(403, "auth.email_not_verified");
  }

  @Test
  @DisplayName("R-001-11 other the first token creates one User, later tokens reuse it; e-mail lowercase")
  void r00111() {
    U u = account("Mojca Kovač", "Mojca.Kovac@Example.COM", true);
    String first = u.userId();
    u.refresh();
    var me = u.get("/me").is(200).json();
    assertThat(me.path("id").asText()).isEqualTo(first);
    assertThat(me.path("email").asText()).isEqualTo("mojca.kovac@example.com");
  }

  @Test
  @DisplayName("R-001-12 invalid a garbage token and a token expired 10 minutes ago are refused")
  void r00112() {
    U ana = user("Ana Novak", "ana@acme.com");
    Instant now = Instant.now();
    setValidSince(ana.uid, now.minusSeconds(7200));
    send("GET", "/groups", "abc.def.ghi", null, Map.of()).is(401);
    send("GET", "/groups", forged(PROJECT, ana.uid, ana.email, now.minusSeconds(4200), now.minusSeconds(600)), null, Map.of()).is(401);
  }

  @Test
  @DisplayName("R-002-01 other the name ' Moja  skupina ' is stored as 'Moja  skupina' with the creator as Owner")
  void r00201() {
    U ana = user("Ana Novak", "ana@acme.com");
    var g = ana.post("/groups", obj("name", " Moja  skupina ")).is(201).json();
    assertThat(g.path("name").asText()).isEqualTo("Moja  skupina");
    assertThat(g.path("role").asText()).isEqualTo("OWNER");
  }

  @Test
  @DisplayName("R-002-02 boundary a name of 99 characters is accepted, of 150 refused")
  void r00202() {
    U ana = user("Ana Novak", "ana@acme.com");
    ana.post("/groups", obj("name", chars(99, 'x'))).is(201);
    ana.post("/groups", obj("name", chars(150, 'y'))).is(422);
    assertThat(list(ana.get("/groups").is(200).json())).hasSize(1);
  }

  @Test
  @DisplayName("R-002-03 combination 'BETA ' (other case, trailing space) is refused for the Owner of Beta")
  void r00203() {
    U ana = user("Ana Novak", "ana@acme.com");
    group(ana, "Beta");
    ana.post("/groups", obj("name", "BETA ")).is(409, "group.name_taken");
  }

  @Test
  @DisplayName("R-002-04 other 40 Groups can be created")
  void r00204() {
    U ana = user("Ana Novak", "ana@acme.com");
    for (int i = 0; i < 40; i++) {
      group(ana, "G" + i);
    }
    assertThat(list(ana.get("/groups").is(200).json())).hasSize(40);
  }

  @Test
  @DisplayName("R-002-05 role a Viewer sees the Group with Role Viewer and no Group of others")
  void r00205() {
    U ana = user("Ana Novak", "ana@acme.com");
    G acme = group(ana, "Acme");
    U cene = user("Cene Zupan", "cene@acme.com");
    group(cene, "Cenetova");
    U viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");
    var groups = list(viola.get("/groups").is(200).json());
    assertThat(groups).hasSize(1);
    assertThat(groups.get(0).path("role").asText()).isEqualTo("VIEWER");
  }

  @Test
  @DisplayName("R-002-06 other a User who is no Member of any Group gets an empty Group list")
  void r00206() {
    U ana = user("Ana Novak", "ana@acme.com");
    G acme = group(ana, "Acme");
    U boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
    ana.delete(acme.p() + "/members/" + memberId(ana, acme, "boris@acme.com")).is(204);
    assertThat(list(boris.get("/groups").is(200).json())).isEmpty();
  }

  @Test
  @DisplayName("R-002-07 other three Owners may each have a Group named Projekt")
  void r00207() {
    for (String who : new String[] {"a", "b", "c"}) {
      U u = user("Oseba " + who, who + "@acme.com");
      group(u, "Projekt");
    }
  }

  @Test
  @DisplayName("BR-05 other the Group name ŠOLA is refused next to Šola of the same Owner")
  void br05() {
    U ana = user("Ana Novak", "ana@acme.com");
    group(ana, "Šola");
    ana.post("/groups", obj("name", "ŠOLA")).is(409, "group.name_taken");
  }
}
