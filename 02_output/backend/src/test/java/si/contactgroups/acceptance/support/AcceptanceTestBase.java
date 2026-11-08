package si.contactgroups.acceptance.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import si.contactgroups.support.PostgresSupport;

/**
 * Base of the acceptance tests of the backend: the whole application on a random port, the
 * throwaway PostgreSQL container and the Firebase Auth emulator (environments.md, "test"). Every
 * test starts with an empty database, no emulator accounts and the real time. Requests go only
 * through the REST interface of {@code docs/02_contracts/openapi.yaml}.
 *
 * <p>Environment of this context: the defaults of environments.md, except {@code
 * RATE_LIMIT_PER_MINUTE} (raised, so that a test that sets up 60 Contacts is not limited; the limit
 * itself is tested with the default in {@code Us018RateLimitAcceptanceIT}) and {@code
 * TRASH_PURGE_CRON} (every second, so that the clean-up of BR-04 runs within a test).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(AcceptanceClockConfig.class)
public abstract class AcceptanceTestBase {

  /** The 11 predefined fields of a Contact (R-003-02), in the order of the contract. */
  public static final List<String> FIELDS =
      List.of(
          "firstName",
          "lastName",
          "email",
          "phone",
          "company",
          "jobTitle",
          "street",
          "postalCode",
          "city",
          "country",
          "notes");

  private static final Set<String> INPUT_KEYS = Set.of("tags", "attributes", "version");

  @DynamicPropertySource
  static void environment(DynamicPropertyRegistry registry) {
    Emulator.ensureStarted();
    registry.add("DB_URL", PostgresSupport::jdbcUrl);
    registry.add("DB_MIGRATION_USER", PostgresSupport::ownerUser);
    registry.add("DB_MIGRATION_PASSWORD", PostgresSupport::ownerPassword);
    registry.add("DB_APP_PASSWORD", () -> PostgresSupport.APP_PASSWORD);
    registry.add("FIREBASE_PROJECT_ID", () -> Emulator.PROJECT_ID);
    registry.add("CORS_ALLOWED_ORIGIN", () -> "http://localhost:5173");
    registry.add("RATE_LIMIT_PER_MINUTE", () -> "1000000");
    registry.add("TRASH_PURGE_CRON", () -> "* * * * * *");
    registry.add("REQUIRE_HTTPS", () -> "false");
  }

  @LocalServerPort private int port;

  @Autowired protected MutableClock clock;

  protected Http http;

  @BeforeEach
  void startFromNothing() {
    clock.reset();
    Db.truncateAll();
    Emulator.clear();
    http = new Http(port);
  }

  // ---- people ---------------------------------------------------------------------------------

  /** A person with a verified e-mail who has signed in once ({@code GET /me}). */
  protected TestUser user(String displayName, String email) {
    TestUser user = account(displayName, email, true);
    user.get("/me").expect(200);
    return user;
  }

  /** A person with an account in Firebase only (the product has not seen a token yet). */
  protected TestUser account(String displayName, String email, boolean verified) {
    String uid = Emulator.createAccount(email, TestUser.PASSWORD, displayName, verified);
    return new TestUser(http, email, displayName, uid, Emulator.signIn(email, TestUser.PASSWORD));
  }

  /** Calls without any token. */
  protected Resp anonymous(String method, String path, Object body) {
    return http.send(method, path, null, body);
  }

  // ---- Groups and Members -----------------------------------------------------------------------

  protected Grp group(TestUser owner, String name) {
    Resp created = owner.post("/groups", Json.obj("name", name)).expect(201);
    return new Grp(created.id(), created.json().path("name").asText());
  }

  /** Makes {@code user} a Member of the Group with {@code role} through an Invitation (US-014). */
  protected void addMember(Grp group, TestUser owner, TestUser user, String role) {
    owner.post(group.invitations(), Json.obj("email", user.email(), "role", role)).expect(201);
    JsonNode groups = user.get("/groups").expect(200).json();
    assertThat(Json.field(groups, "id")).as("%s joined %s", user, group.name()).contains(group.id());
  }

  /** Creates a verified person and adds them to the Group with {@code role}. */
  protected TestUser member(Grp group, TestUser owner, String displayName, String email, String role) {
    TestUser user = user(displayName, email);
    addMember(group, owner, user, role);
    return user;
  }

  protected String roleIn(TestUser user, Grp group) {
    for (JsonNode g : Json.list(user.get("/groups").expect(200).json())) {
      if (g.path("id").asText().equals(group.id())) {
        return g.path("role").asText();
      }
    }
    return null;
  }

  // ---- Contacts ---------------------------------------------------------------------------------

  /** A Contact body from key-value pairs (fields, {@code tags}, {@code attributes}). */
  protected static ObjectNode contactBody(Object... keyValues) {
    return Json.obj(keyValues);
  }

  /** An Attribute pair list from alternating keys and values. */
  protected static List<JsonNode> attrs(String... keysAndValues) {
    List<JsonNode> out = new ArrayList<>();
    for (int i = 0; i < keysAndValues.length; i += 2) {
      out.add(Json.obj("key", keysAndValues[i], "value", keysAndValues[i + 1]));
    }
    return out;
  }

  protected JsonNode createContact(TestUser user, Grp group, Object... keyValues) {
    return user.post(group.contacts(), contactBody(keyValues)).expect(201).json();
  }

  protected JsonNode getContact(TestUser user, Grp group, String contactId) {
    return user.get(group.contact(contactId)).expect(200).json();
  }

  /** The edit body that keeps everything of {@code current} and applies {@code changes}. */
  protected static ObjectNode editBody(JsonNode current, Object... changes) {
    ObjectNode body = Json.MAPPER.createObjectNode();
    for (String field : FIELDS) {
      if (current.has(field)) {
        body.set(field, current.get(field));
      }
    }
    for (String key : INPUT_KEYS) {
      if (current.has(key)) {
        body.set(key, current.get(key));
      }
    }
    ObjectNode change = Json.obj(changes);
    change.properties().forEach(e -> body.set(e.getKey(), e.getValue()));
    return body;
  }

  /** Saves {@code changes} on top of the Contact as {@code user} last read it. */
  protected Resp save(TestUser user, Grp group, JsonNode current, Object... changes) {
    return user.put(group.contact(current.path("id").asText()), editBody(current, changes));
  }

  /** Reads the Contact, applies the changes and saves; asserts 200 and returns the new state. */
  protected JsonNode edit(TestUser user, Grp group, String contactId, Object... changes) {
    return save(user, group, getContact(user, group, contactId), changes).expect(200).json();
  }

  protected void moveToTrash(TestUser user, Grp group, String contactId) {
    user.post(group.toTrash(contactId)).expect(204);
  }

  // ---- lists ------------------------------------------------------------------------------------

  protected JsonNode contactPage(TestUser user, Grp group, String query) {
    return user.get(group.contacts(query)).expect(200).json();
  }

  protected List<JsonNode> contactItems(TestUser user, Grp group, String query) {
    return Json.list(contactPage(user, group, query).path("items"));
  }

  /** The ids of all Contacts of every page of the list for {@code query}. */
  protected List<String> allContactIds(TestUser user, Grp group, String query) {
    List<String> ids = new ArrayList<>();
    for (int page = 1; page < 500; page++) {
      String q = (query.isEmpty() ? "" : query + "&") + "page=" + page;
      List<JsonNode> items = contactItems(user, group, q);
      if (items.isEmpty()) {
        break;
      }
      items.forEach(item -> ids.add(item.path("id").asText()));
    }
    return ids;
  }

  protected List<JsonNode> history(TestUser user, Grp group, String contactId) {
    return Json.list(user.get(group.history(contactId)).expect(200).json().path("items"));
  }

  protected List<JsonNode> activity(TestUser user, Grp group, String query) {
    return Json.list(user.get(group.activity(query)).expect(200).json().path("items"));
  }

  protected static List<String> actions(List<JsonNode> entries) {
    return entries.stream().map(e -> e.path("action").asText()).toList();
  }

  protected List<JsonNode> tagList(TestUser user, Grp group) {
    return Json.list(user.get(group.tags()).expect(200).json());
  }

  protected String tagId(TestUser user, Grp group, String name) {
    return tagList(user, group).stream()
        .filter(t -> t.path("name").asText().equals(name))
        .map(t -> t.path("id").asText())
        .findFirst()
        .orElseThrow(() -> new AssertionError("no Tag " + name));
  }

  // ---- time and concurrency ---------------------------------------------------------------------

  /** Waits until the clean-up job (every second in this context) made {@code condition} true. */
  protected static void awaitCleanUp(Callable<Boolean> condition) {
    Awaitility.await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(250)).until(condition);
  }

  /** Lets the clean-up job run several times (it runs every second in this context). */
  protected static void letCleanUpRun() {
    try {
      Thread.sleep(3500);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /** Runs the calls at the same time and returns their results in order. */
  @SafeVarargs
  protected static <T> List<T> atOnce(Callable<T>... calls) throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(calls.length);
    try {
      CountDownLatch start = new CountDownLatch(1);
      List<Future<T>> futures = new ArrayList<>();
      for (Callable<T> call : calls) {
        futures.add(
            pool.submit(
                () -> {
                  start.await();
                  return call.call();
                }));
      }
      start.countDown();
      List<T> results = new ArrayList<>();
      for (Future<T> future : futures) {
        results.add(future.get());
      }
      return results;
    } finally {
      pool.shutdownNow();
    }
  }

  /** A string of {@code n} times {@code c}. */
  protected static String chars(int n, char c) {
    return String.valueOf(c).repeat(n);
  }

  /** A valid e-mail address of exactly {@code length} characters ending in {@code @acme.com}. */
  protected static String emailOfLength(int length) {
    String domain = "@acme.com";
    return chars(length - domain.length(), 'a') + domain;
  }
}
