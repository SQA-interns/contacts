package si.contactgroups.hidden;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Hidden cases of US-016, US-017, US-018 (default environment) and BR-03. */
class H16HistorySecurityHiddenIT extends HiddenBase {

  U ana;
  G acme;

  @BeforeEach
  void setUp() {
    ana = user("Ana Novak", "ana@acme.com");
    acme = group(ana, "Acme");
  }

  @Test
  @DisplayName("R-016-01 other the creation entry holds the company and the city")
  void r01601() {
    String c = contact(ana, acme, "company", "Sonce d.o.o.", "city", "Ptuj");
    JsonNode e = history(ana, acme, c).get(0);
    assertThat(e.path("action").asText()).isEqualTo("CONTACT_CREATED");
    assertThat(e.path("details").toString()).contains("Sonce d.o.o.").contains("Ptuj");
  }

  @Test
  @DisplayName("R-016-02 combination an edit of city and phone lists both and not the unchanged company")
  void r01602() {
    String c = contact(ana, acme, "company", "Sonce d.o.o.", "city", "Ptuj");
    edit(ana, acme, c, "city", "Ormož", "phone", "02 111 222");
    String d = history(ana, acme, c).get(0).path("details").toString();
    assertThat(d).contains("Ptuj").contains("Ormož").contains("02 111 222").doesNotContain("Sonce");
  }

  @Test
  @DisplayName("R-016-03 other three entries come newest first with user name and e-mail")
  void r01603() {
    String c = contact(ana, acme, "firstName", "M");
    edit(ana, acme, c, "city", "A");
    edit(ana, acme, c, "city", "B");
    List<JsonNode> h = history(ana, acme, c);
    assertThat(actions(h)).containsExactly("CONTACT_EDITED", "CONTACT_EDITED", "CONTACT_CREATED");
    assertThat(Instant.parse(h.get(0).path("occurredAt").asText())).isAfterOrEqualTo(Instant.parse(h.get(2).path("occurredAt").asText()));
    assertThat(h.get(0).path("userEmail").asText()).isEqualTo("ana@acme.com");
  }

  @Test
  @DisplayName("R-016-04 role an Editor reads the History, a Viewer does not")
  void r01604() {
    String c = contact(ana, acme, "firstName", "M");
    U boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
    U viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");
    boris.get(acme.contact(c) + "/history").is(200);
    viola.get(acme.contact(c) + "/history").is(403);
  }

  @Test
  @DisplayName("R-016-05 invalid a POST to the History is refused and adds nothing")
  void r01605() {
    String c = contact(ana, acme, "firstName", "M");
    assertThat(ana.post(acme.contact(c) + "/history", obj("action", "CONTACT_EDITED")).status()).isBetween(400, 499);
    assertThat(history(ana, acme, c)).hasSize(1);
  }

  @Test
  @DisplayName("R-016-06 other an entry keeps the e-mail of the time after the person changed the display name")
  void r01606() {
    U boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
    String c = contact(boris, acme, "firstName", "M");
    emulator("POST", "/identitytoolkit.googleapis.com/v1/projects/" + PROJECT + "/accounts:update", obj("localId", boris.uid, "displayName", "Boris Drugi"), true);
    boris.refresh().get("/me").is(200);
    JsonNode e = history(ana, acme, c).get(0);
    assertThat(e.path("userName").asText()).isEqualTo("Boris Kos");
    assertThat(e.path("userEmail").asText()).isEqualTo("boris@acme.com");
  }

  @Test
  @DisplayName("R-016-07 other an entry of two years ago is still listed")
  void r01607() {
    clock.setNow(Instant.now().minus(Duration.ofDays(730)));
    String c = contact(ana, acme, "firstName", "M");
    clock.reset();
    assertThat(actions(history(ana, acme, c))).containsExactly("CONTACT_CREATED");
  }

  @Test
  @DisplayName("R-016-08 other after a restore new edits are added to the earlier entries")
  void r01608() {
    String c = contact(ana, acme, "firstName", "M");
    trash(ana, acme, c);
    ana.post(acme.p() + "/trash/contacts/" + c + "/restore").is(200);
    edit(ana, acme, c, "city", "Bled");
    assertThat(actions(history(ana, acme, c))).containsExactly("CONTACT_EDITED", "CONTACT_RESTORED", "CONTACT_DELETED", "CONTACT_CREATED");
  }

  @Test
  @DisplayName("R-017-01 other an entry of the activity list names the Contact")
  void r01701() {
    String c = contact(ana, acme, "firstName", "M");
    assertThat(activity(ana, acme, "").get(0).path("contactId").asText()).isEqualTo(c);
  }

  @Test
  @DisplayName("R-017-02 other the user filter Boris gives only his two entries")
  void r01702() {
    U boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
    contact(ana, acme, "firstName", "A");
    String c = contact(boris, acme, "firstName", "B");
    edit(boris, acme, c, "city", "Bled");
    List<JsonNode> a = activity(ana, acme, "userId=" + boris.userId());
    assertThat(a).hasSize(2);
  }

  @Test
  @DisplayName("R-017-03 other the action filter TAG_ADDED gives only Tag entries")
  void r01703() {
    String c = contact(ana, acme, "firstName", "M");
    edit(ana, acme, c, "tags", arr("x"));
    assertThat(actions(activity(ana, acme, "action=TAG_ADDED"))).containsExactly("TAG_ADDED");
  }

  @Test
  @DisplayName("R-017-04 boundary in summer time 2026-07-01 22:30 UTC belongs to 2 July")
  void r01704() {
    clock.setNow(Instant.parse("2026-07-01T22:30:00Z"));
    contact(ana, acme, "firstName", "Poleti");
    clock.reset();
    assertThat(activity(ana, acme, "from=2026-07-01&to=2026-07-01")).isEmpty();
    assertThat(activity(ana, acme, "from=2026-07-02&to=2026-07-02")).hasSize(1);
  }

  @Test
  @DisplayName("R-017-05 combination user, action and dates together")
  void r01705() {
    U boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
    clock.setNow(Instant.parse("2026-05-10T10:00:00Z"));
    String c = contact(boris, acme, "firstName", "M");
    edit(boris, acme, c, "city", "A");
    clock.setNow(Instant.parse("2026-05-12T10:00:00Z"));
    edit(boris, acme, c, "city", "B");
    clock.reset();
    String q = "userId=" + boris.userId() + "&action=CONTACT_EDITED&from=2026-05-10&to=2026-05-10";
    assertThat(activity(ana, acme, q)).hasSize(1);
  }

  @Test
  @DisplayName("R-017-06 invalid a start one day after the end is refused")
  void r01706() {
    ana.get(acme.p() + "/activity?from=2026-11-02&to=2026-11-01").is(422, "activity.range_invalid");
  }

  @Test
  @DisplayName("R-017-07 other a user filter without entries gives an empty list")
  void r01707() {
    contact(ana, acme, "firstName", "M");
    U boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
    var p = ana.get(acme.p() + "/activity?userId=" + boris.userId() + "&action=TAG_ADDED").is(200).json();
    assertThat(p.path("items")).isEmpty();
  }

  @Test
  @DisplayName("R-017-08 role a Viewer is refused also with filters")
  void r01708() {
    U viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");
    viola.get(acme.p() + "/activity?action=CONTACT_CREATED").is(403);
  }

  @Test
  @DisplayName("R-017-09 other the entries of Beta are not in the list of Acme")
  void r01709() {
    G beta = group(ana, "Beta");
    contact(ana, beta, "firstName", "Betin");
    assertThat(activity(ana, acme, "")).isEmpty();
  }

  @Test
  @DisplayName("R-018-01 invalid an empty Bearer and a Basic header are answered 401")
  void r01801() {
    send("GET", acme.contacts(), null, null, Map.of("Authorization", "Bearer ")).is(401);
    send("GET", acme.contacts(), null, null, Map.of("Authorization", "Basic YW5hOnBhc3M=")).is(401);
  }

  @Test
  @DisplayName("R-018-02 other members, activity and exports of Acme are 404 for a non-Member")
  void r01802() {
    U x = user("Tuja Oseba", "tuja@x.si");
    x.get(acme.p() + "/members").is(404);
    x.get(acme.p() + "/activity").is(404);
    export(x, acme, "en", obj("filter", obj())).is(404);
  }

  @Test
  @DisplayName("R-018-03 role a Viewer's Tag deletion is 403 and the Tag stays")
  void r01803() {
    contact(ana, acme, "firstName", "M", "tags", arr("x"));
    U viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");
    viola.delete(acme.p() + "/tags/" + tagId(ana, acme, "x")).is(403);
    assertThat(tags(ana, acme)).hasSize(1);
  }

  @Test
  @DisplayName("R-018-04 invalid a groupId in the body of an edit never moves the Contact")
  void r01804() {
    G beta = group(ana, "Beta");
    String c = contact(ana, acme, "firstName", "M");
    var body = editBody(read(ana, acme, c), "city", "Bled");
    body.put("groupId", beta.id());
    ana.put(acme.contact(c), body);
    assertThat(total(ana, beta, "")).isZero();
    read(ana, acme, c);
  }

  @Test
  @DisplayName("R-018-05 other the Versions of a Contact of Beta under Acme are 404")
  void r01805() {
    G beta = group(ana, "Beta");
    String c = contact(ana, beta, "firstName", "M");
    ana.get(acme.contact(c) + "/versions").is(404);
    ana.get(acme.contact(c) + "/history").is(404);
  }

  @Test
  @DisplayName("R-018-06 other an unverified User gets 403 for the Member list")
  void r01806() {
    U x = account("Neža Kos", "neza@acme.com", false);
    x.get(acme.p() + "/members").is(403, "auth.email_not_verified");
  }

  @Test
  @DisplayName("R-018-07 other the health check answers JSON with only the status")
  void r01807() {
    Resp r = send("GET", "/health", null, null, Map.of()).is(200);
    assertThat(r.json().size()).isEqualTo(1);
    assertThat(r.json().path("status").asText()).isEqualTo("UP");
  }

  @Test
  @DisplayName("R-018-08 other the published description contains the Group and Contact paths")
  void r01808() {
    String doc = ana.get("/openapi.json").is(200).body();
    assertThat(doc).contains("/groups").contains("contacts").contains("exports");
  }

  @Test
  @DisplayName("R-018-09 other an unknown path answers 404 in the error format without a trace")
  void r01809() {
    Resp r = ana.get("/nicesar/tukaj").is(404);
    assertThat(r.json().path("code").asText()).isNotBlank();
    assertThat(r.body()).doesNotContain("Exception").doesNotContain("\tat ");
  }

  @Test
  @DisplayName("R-018-11 boundary a body of 1 MB minus 1 byte is accepted")
  void r01811() {
    String start = "{\"firstName\":\"Skoraj\"";
    String json = start + " ".repeat(1024 * 1024 - 1 - start.length() - 1) + "}";
    send("POST", acme.contacts(), ana.token, json, Map.of()).is(201);
  }

  @Test
  @DisplayName("R-018-14 other a Tag value <img src=x onerror=alert(1)> in an Attribute is kept as text")
  void r01814() {
    String c = contact(ana, acme, "firstName", "M", "attributes", attrs("html", "<img src=x onerror=alert(1)>"));
    assertThat(read(ana, acme, c).path("attributes").get(0).path("value").asText()).isEqualTo("<img src=x onerror=alert(1)>");
  }

  @Test
  @DisplayName("R-018-15 invalid an XML body with a JSON content type is refused")
  void r01815() {
    Resp r = send("POST", acme.contacts(), ana.token, "<contact><firstName>M</firstName></contact>", Map.of());
    assertThat(r.status()).isEqualTo(400);
    assertThat(total(ana, acme, "")).isZero();
  }

  @Test
  @DisplayName("R-018-16 other the own origin may PUT, a foreign origin gets no permission")
  void r01816() {
    Resp own = send("OPTIONS", acme.contacts(), null, null, Map.of("Origin", "http://localhost:5173", "Access-Control-Request-Method", "PUT"));
    Resp foreign = send("OPTIONS", acme.contacts(), null, null, Map.of("Origin", "http://localhost:5174", "Access-Control-Request-Method", "PUT"));
    assertThat(own.headers().firstValue("Access-Control-Allow-Origin")).hasValue("http://localhost:5173");
    assertThat(foreign.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
  }

  @Test
  @DisplayName("BR-03 other no request can delete an entry of the activity list")
  void br03() {
    contact(ana, acme, "firstName", "M");
    String id = activity(ana, acme, "").get(0).path("id").asText();
    assertThat(ana.delete(acme.p() + "/activity/" + id).status()).isBetween(400, 499);
    assertThat(ana.delete(acme.p() + "/activity").status()).isBetween(400, 499);
    assertThat(activity(ana, acme, "")).hasSize(1);
  }
}
