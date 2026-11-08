package si.contactgroups.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.contactgroups.acceptance.support.AcceptanceTestBase;
import si.contactgroups.acceptance.support.Contract;
import si.contactgroups.acceptance.support.Contract.Operation;
import si.contactgroups.acceptance.support.Db;
import si.contactgroups.acceptance.support.Emulator;
import si.contactgroups.acceptance.support.Exports;
import si.contactgroups.acceptance.support.Grp;
import si.contactgroups.acceptance.support.Json;
import si.contactgroups.acceptance.support.Resp;
import si.contactgroups.acceptance.support.TestUser;

/**
 * US-018 Secured REST interface and Group isolation (with the test defaults of {@link
 * AcceptanceTestBase}). Rate limit and log: {@code Us018RateLimitOwnContextAcceptanceIT}; HTTPS:
 * {@code Us018HttpsOwnContextAcceptanceIT}.
 */
class Us018SecurityAcceptanceIT extends AcceptanceTestBase {

  private static final int ONE_MB = 1024 * 1024;

  private TestUser ana;
  private Grp acme;
  private String anaNovak;

  @BeforeEach
  void anaOwnsAcmeWithAContact() {
    ana = user("Ana Novak", "ana@acme.com");
    acme = group(ana, "Acme");
    anaNovak =
        createContact(
                ana, acme, "firstName", "Ana", "lastName", "Novak", "email", "ana.novak@acme.com")
            .path("id")
            .asText();
  }

  private static void assertNoContactData(Resp answer) {
    assertThat(answer.body()).doesNotContain("Novak").doesNotContain("ana.novak@acme.com");
  }

  private static void assertErrorFormat(Resp answer) {
    JsonNode body = answer.json();
    assertThat(body.path("code").asText()).as("code in %s", answer.body()).isNotBlank();
    assertThat(body.path("message").asText()).as("message in %s", answer.body()).isNotBlank();
    assertThat(body.has("trace") || body.has("stackTrace") || body.has("exception")).isFalse();
    assertThat(answer.body())
        .doesNotContain("\tat ")
        .doesNotContain("si.contactgroups.")
        .doesNotContain("Exception");
  }

  private Resp call(TestUser user, Operation op, String groupId) {
    String token = user == null ? null : user.token();
    return http.send(op.method(), op.concrete(groupId), token, op.hasBody() ? Json.obj() : null);
  }

  @Test
  @DisplayName("AC-018-01 every Contact endpoint called without a token answers 401 and no data")
  void ac_018_01_contact_endpoints_need_a_token() {
    List<Operation> contactOps =
        Contract.operations().stream().filter(op -> op.path().contains("/contacts")).toList();
    assertThat(contactOps).isNotEmpty();

    for (Operation op : contactOps) {
      Resp answer = call(null, op, acme.id());
      assertThat(answer.status()).as("%s %s", op.method(), op.path()).isEqualTo(401);
      assertNoContactData(answer);
    }
  }

  @Test
  @DisplayName("AC-018-02 a token of a deleted account and an expired token are answered 401")
  void ac_018_02_deleted_account_and_expired_token() {
    TestUser boris = user("Boris Kos", "boris@acme.com");
    String borisToken = boris.token();
    Instant now = Instant.now();
    Emulator.setValidSince(ana.uid(), now.minusSeconds(5 * 3600));
    String expired =
        Emulator.forgedToken(
            Emulator.PROJECT_ID,
            ana.uid(),
            ana.email(),
            now.minusSeconds(3 * 3600),
            now.minusSeconds(2 * 3600));

    Emulator.deleteAccount(boris.uid());

    Resp deleted = http.send("GET", "/groups", borisToken, null).expect(401);
    Resp old = http.send("GET", acme.contacts(), expired, null).expect(401);
    assertNoContactData(old);
    assertErrorFormat(deleted);
  }

  @Test
  @DisplayName(
      "AC-018-03 Boris (no Member) gets 404 and no Contact data for GET /groups/{Acme}/contacts")
  void ac_018_03_non_member_404() {
    TestUser boris = user("Boris Kos", "boris@acme.com");

    Resp answer = boris.get(acme.contacts()).expect(404);

    assertNoContactData(answer);
    assertErrorFormat(answer);
  }

  @Test
  @DisplayName(
      "AC-018-04 every endpoint with a Group in its path answers 404 and no data to a non-Member")
  void ac_018_04_isolation_over_the_whole_contract() {
    TestUser boris = user("Boris Kos", "boris@acme.com");
    List<Operation> groupOps =
        Contract.operations().stream().filter(op -> op.path().contains("{groupId}")).toList();
    assertThat(groupOps.size()).isGreaterThan(25);

    List<String> wrong = new ArrayList<>();
    for (Operation op : groupOps) {
      Resp answer = call(boris, op, acme.id());
      if (answer.status() != 404
          || answer.body().contains("Novak")
          || answer.body().contains("Acme")) {
        wrong.add(op.method() + " " + op.path() + " -> " + answer.status());
      }
    }

    assertThat(wrong).isEmpty();
    assertThat(contactPage(ana, acme, "").path("total").asInt()).isEqualTo(1);
    assertThat(roleIn(ana, acme)).isEqualTo("OWNER");
  }

  @Test
  @DisplayName("AC-018-05 a Viewer's export, create and Trash calls answer 403 and change nothing")
  void ac_018_05_viewer_403() {
    TestUser viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");

    Exports.byIds(viola, acme, "en", List.of(anaNovak)).expect(403);
    viola.post(acme.contacts(), contactBody("firstName", "Nov")).expect(403);
    viola.post(acme.toTrash(anaNovak)).expect(403);
    viola.get(acme.trash()).expect(403);

    assertThat(contactPage(ana, acme, "").path("total").asInt()).isEqualTo(1);
    assertThat(activity(ana, acme, "action=CONTACTS_EXPORTED")).isEmpty();
  }

  @Test
  @DisplayName(
      "AC-018-06 a Group named in the body is ignored or rejected; the Contact is never in Beta")
  void ac_018_06_group_from_path_only() {
    Grp beta = group(ana, "Beta");

    Resp answer =
        ana.post(acme.contacts(), contactBody("firstName", "Vrinjen", "group", beta.id()));

    assertThat(answer.status()).isIn(201, 400, 422);
    assertThat(contactPage(ana, beta, "").path("total").asInt()).isZero();
    if (answer.status() == 201) {
      assertThat(contactPage(ana, acme, "q=Vrinjen").path("total").asInt()).isEqualTo(1);
    }
  }

  @Test
  @DisplayName("AC-018-07 Contact C of Beta requested under the path of Acme answers 404")
  void ac_018_07_object_of_other_group_404() {
    Grp beta = group(ana, "Beta");
    String c =
        createContact(ana, beta, "firstName", "Cene", "lastName", "Betin").path("id").asText();

    Resp answer = ana.get(acme.contact(c)).expect(404);
    ana.put(acme.contact(c), contactBody("firstName", "X", "version", 1)).expect(404);
    ana.post(acme.toTrash(c)).expect(404);

    assertThat(answer.body()).doesNotContain("Betin");
    assertThat(getContact(ana, beta, c).path("firstName").asText()).isEqualTo("Cene");
  }

  @Test
  @DisplayName("AC-018-08 a User with an unverified e-mail gets 403 on a Group endpoint")
  void ac_018_08_unverified_403() {
    TestUser boris = account("Boris Kos", "boris@acme.com", false);

    boris.get(acme.contacts()).expectError(403, "auth.email_not_verified");
  }

  @Test
  @DisplayName(
      "AC-018-09 the health check without token answers 200 with the status and nothing else")
  void ac_018_09_health_public_and_minimal() {
    Resp answer = anonymous("GET", "/health", null).expect(200);

    JsonNode body = answer.json();
    assertThat(body.path("status").asText()).isEqualTo("UP");
    List<String> keys = new ArrayList<>();
    body.fieldNames().forEachRemaining(keys::add);
    assertThat(keys).containsExactly("status");
  }

  @Test
  @DisplayName("AC-018-10 the published OpenAPI document lists every endpoint of the contract")
  void ac_018_10_openapi_lists_every_endpoint() {
    JsonNode published = ana.get("/openapi.json").expect(200).json();
    assertThat(published.path("openapi").asText()).startsWith("3.");

    Set<String> offered = new HashSet<>();
    Iterator<Map.Entry<String, JsonNode>> paths = published.path("paths").fields();
    while (paths.hasNext()) {
      Map.Entry<String, JsonNode> path = paths.next();
      path.getValue()
          .fieldNames()
          .forEachRemaining(
              m ->
                  offered.add(
                      m.toUpperCase(Locale.ROOT) + " " + Contract.normalise(path.getKey())));
    }
    List<String> missing =
        Contract.operations().stream()
            // the health check lies outside /api/v1 and the description does not describe itself
            .filter(op -> !op.path().equals("/health") && !op.path().equals("/openapi.json"))
            .map(op -> op.method() + " " + op.shape())
            .filter(op -> !offered.contains(op))
            .toList();

    assertThat(missing).isEmpty();
  }

  @Test
  @DisplayName("AC-018-17 a request with a body that is not JSON is refused in the error format")
  void ac_018_17_non_json_body_refused() {
    Resp text =
        ana.send("POST", acme.contacts(), "firstName=Ana", Map.of("Content-Type", "text/plain"));
    Resp broken =
        ana.send(
            "POST",
            acme.contacts(),
            "{\"firstName\": ",
            Map.of("Content-Type", "application/json"));

    assertThat(text.status()).isBetween(400, 499);
    assertThat(broken.status()).isEqualTo(400);
    assertErrorFormat(text);
    assertErrorFormat(broken);
    assertThat(contactPage(ana, acme, "").path("total").asInt()).isEqualTo(1);
  }

  @Test
  @DisplayName("AC-018-11 errors have a code and a message and no stack trace")
  void ac_018_11_error_format() {
    TestUser boris = user("Boris Kos", "boris@acme.com");

    assertErrorFormat(anonymous("GET", "/groups", null).expect(401));
    assertErrorFormat(boris.get(acme.contacts()).expect(404));
    assertErrorFormat(ana.post(acme.contacts(), contactBody("email", "ana@")).expect(422));
    assertErrorFormat(ana.post(acme.contacts(), contactBody("nepoznano", "x")).expect(400));
  }

  @Test
  @DisplayName("AC-018-13 a body of 1 MB + 1 byte to a create endpoint is refused; nothing stored")
  void ac_018_13_body_over_1_mb_refused() {
    byte[] body = paddedContact(ONE_MB + 1, "Prevelik");

    ana.send("POST", acme.contacts(), body, Map.of()).expectError(413, "request.too_large");

    assertThat(contactPage(ana, acme, "q=Prevelik").path("total").asInt()).isZero();
  }

  @Test
  @DisplayName("EX-018-04 a body of exactly 1 MB is accepted")
  void ex_018_04_body_of_1_mb_accepted() {
    byte[] body = paddedContact(ONE_MB, "Ravnoprav");

    ana.send("POST", acme.contacts(), body, Map.of()).expect(201);

    assertThat(contactPage(ana, acme, "q=Ravnoprav").path("total").asInt()).isEqualTo(1);
  }

  /** A valid create body of exactly {@code size} bytes: a first name and JSON whitespace. */
  private static byte[] paddedContact(int size, String firstName) {
    String start = "{\"firstName\":\"" + firstName + "\"";
    String end = "}";
    int padding = size - start.getBytes(StandardCharsets.UTF_8).length - end.length();
    return (start + " ".repeat(padding) + end).getBytes(StandardCharsets.UTF_8);
  }

  @Test
  @DisplayName("AC-018-14 a request with Origin https://evil.example gets no CORS permission")
  void ac_018_14_foreign_origin_no_cors() {
    Resp preflight =
        http.send(
            "OPTIONS",
            acme.contacts(),
            null,
            null,
            Map.of("Origin", "https://evil.example", "Access-Control-Request-Method", "GET"));
    Resp request = ana.send("GET", acme.contacts(), null, Map.of("Origin", "https://evil.example"));
    Resp own =
        http.send(
            "OPTIONS",
            acme.contacts(),
            null,
            null,
            Map.of("Origin", "http://localhost:5173", "Access-Control-Request-Method", "GET"));

    assertThat(preflight.header("Access-Control-Allow-Origin")).isNull();
    assertThat(request.header("Access-Control-Allow-Origin")).isNull();
    assertThat(own.header("Access-Control-Allow-Origin")).isEqualTo("http://localhost:5173");
  }

  @Test
  @DisplayName("EX-018-01 a Group id that does not exist answers 404 like a Group of others")
  void ex_018_01_missing_group_same_as_foreign() {
    TestUser boris = user("Boris Kos", "boris@acme.com");

    Resp missing = boris.get("/groups/" + Db.randomId() + "/contacts").expect(404);
    Resp foreign = boris.get(acme.contacts()).expect(404);

    assertThat(missing.json()).isEqualTo(foreign.json());
  }

  @Test
  @DisplayName("EX-018-02 a malformed Group id abc answers 404 in the error format")
  void ex_018_02_malformed_id_404() {
    Resp answer = ana.get("/groups/abc/contacts").expect(404);

    assertErrorFormat(answer);
  }

  @Test
  @DisplayName("EX-018-05 a Member who was just removed gets 404 on the next request")
  void ex_018_05_removed_member_404() {
    TestUser boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
    boris.get(acme.contacts()).expect(200);
    String borisId = boris.userId();

    ana.delete(acme.member(borisId)).expect(204);

    assertNoContactData(boris.get(acme.contacts()).expect(404));
  }

  @Test
  @DisplayName("EX-018-06 a method that is not allowed on a path answers 405 in the error format")
  void ex_018_06_method_not_allowed() {
    Resp answer = ana.delete("/groups").expect(405);

    assertErrorFormat(answer);
  }

  @Test
  @DisplayName("EX-018-07 a token of another Firebase project answers 401")
  void ex_018_07_other_project_token() {
    Instant now = Instant.now();
    Emulator.setValidSince(ana.uid(), now.minusSeconds(3600));
    String foreign =
        Emulator.forgedToken(
            "other-project", ana.uid(), ana.email(), now.minusSeconds(60), now.plusSeconds(600));

    assertNoContactData(http.send("GET", acme.contacts(), foreign, null).expect(401));
  }

  @Test
  @DisplayName("EX-018-08 a request for a Group in Trash answers 404")
  void ex_018_08_group_in_trash_404() {
    ana.post(acme.toGroupTrash(), Json.obj("name", "Acme")).expect(204);

    assertNoContactData(ana.get(acme.contacts()).expect(404));
  }
}
