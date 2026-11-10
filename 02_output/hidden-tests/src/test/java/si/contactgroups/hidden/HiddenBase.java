package si.contactgroups.hidden;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Self-contained harness of the hidden suite: the whole backend on a random port, a throwaway
 * PostgreSQL, the Firebase Auth emulator on port 9099, a movable clock in place of the product's
 * {@link Clock}, an empty database and no accounts before every test, the clean-up job every second.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(HiddenBase.ClockConfig.class)
@TestPropertySource(properties = {"RATE_LIMIT_PER_MINUTE=1000000", "REQUIRE_HTTPS=false"})
public abstract class HiddenBase {

  static final ObjectMapper JSON = new ObjectMapper();
  static final String PROJECT = "demo-contact-groups";
  static final String PASSWORD = "Geslo1234";
  static final String APP_PASSWORD = "app-hidden-password";
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6");
  static final GenericContainer<?> EMULATOR = startEmulator();
  static final HttpClient CLIENT = HttpClient.newHttpClient();
  static final List<String> FIELDS =
      List.of("firstName", "lastName", "email", "phone", "company", "jobTitle", "street", "postalCode", "city", "country", "notes");

  static {
    POSTGRES.start();
  }

  @SuppressWarnings("resource")
  private static GenericContainer<?> startEmulator() {
    RuntimeException last = null;
    for (int attempt = 0; attempt < 6; attempt++) {
      try {
        GenericContainer<?> c =
            new GenericContainer<>(
                    new ImageFromDockerfile("contact-groups-emulator-hidden", false)
                        .withFileFromPath(".", Path.of("..", "emulator")))
                .withExposedPorts(9099)
                .withCreateContainerCmdModifier(
                    cmd -> cmd.getHostConfig().withPortBindings(com.github.dockerjava.api.model.PortBinding.parse("9099:9099")))
                .waitingFor(Wait.forLogMessage(".*All emulators ready.*", 1))
                .withStartupTimeout(Duration.ofMinutes(3));
        c.start();
        Runtime.getRuntime().addShutdownHook(new Thread(c::stop));
        return c;
      } catch (RuntimeException e) {
        last = e;
        sleep(5000);
      }
    }
    throw last;
  }

  @DynamicPropertySource
  static void environment(DynamicPropertyRegistry r) {
    r.add("DB_URL", POSTGRES::getJdbcUrl);
    r.add("DB_MIGRATION_USER", POSTGRES::getUsername);
    r.add("DB_MIGRATION_PASSWORD", POSTGRES::getPassword);
    r.add("DB_APP_PASSWORD", () -> APP_PASSWORD);
    r.add("FIREBASE_PROJECT_ID", () -> PROJECT);
    r.add("CORS_ALLOWED_ORIGIN", () -> "http://localhost:5173");
    r.add("TRASH_PURGE_CRON", () -> "* * * * * *");
  }

  /** The clock of the hidden tests: system time plus an offset. */
  public static final class MovableClock extends Clock {
    private volatile Duration offset = Duration.ZERO;

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return Clock.offset(Clock.system(zone), offset);
    }

    @Override
    public Instant instant() {
      return Instant.now().plus(offset);
    }

    void reset() {
      offset = Duration.ZERO;
    }

    void setNow(Instant now) {
      offset = Duration.between(Instant.now(), now);
    }

    void advance(Duration d) {
      offset = offset.plus(d);
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  public static class ClockConfig {
    @Bean
    @Primary
    public MovableClock hiddenTestClock() {
      return new MovableClock();
    }
  }

  @LocalServerPort int port;
  @Autowired MovableClock clock;

  @BeforeEach
  void fresh() throws Exception {
    clock.reset();
    try (Connection c = owner();
        Statement s = c.createStatement()) {
      List<String> tables = new ArrayList<>();
      try (ResultSet rs =
          s.executeQuery("select tablename from pg_tables where schemaname='public' and tablename<>'flyway_schema_history'")) {
        while (rs.next()) {
          tables.add("\"" + rs.getString(1) + "\"");
        }
      }
      if (!tables.isEmpty()) {
        s.execute("truncate table " + String.join(",", tables) + " cascade");
      }
    }
    emulator("DELETE", "/emulator/v1/projects/" + PROJECT + "/accounts", null, true);
  }

  // ---- JSON ----------------------------------------------------------------------------------

  static ObjectNode obj(Object... kv) {
    ObjectNode n = JSON.createObjectNode();
    for (int i = 0; i < kv.length; i += 2) {
      n.set((String) kv[i], JSON.valueToTree(kv[i + 1]));
    }
    return n;
  }

  static ArrayNode arr(Object... values) {
    ArrayNode n = JSON.createArrayNode();
    for (Object v : values) {
      n.add(JSON.<JsonNode>valueToTree(v));
    }
    return n;
  }

  static List<JsonNode> attrs(String... kv) {
    List<JsonNode> out = new ArrayList<>();
    for (int i = 0; i < kv.length; i += 2) {
      out.add(obj("key", kv[i], "value", kv[i + 1]));
    }
    return out;
  }

  static JsonNode parse(String s) {
    try {
      return JSON.readTree(s == null || s.isEmpty() ? "null" : s);
    } catch (Exception e) {
      return JSON.getNodeFactory().textNode(s);
    }
  }

  static List<JsonNode> list(JsonNode a) {
    List<JsonNode> out = new ArrayList<>();
    if (a != null && a.isArray()) {
      a.forEach(out::add);
    }
    return out;
  }

  static List<String> texts(JsonNode a, String field) {
    return list(a).stream().map(n -> field == null ? n.asText() : n.path(field).asText(null)).toList();
  }

  static String enc(String s) {
    return URLEncoder.encode(s, StandardCharsets.UTF_8);
  }

  static String chars(int n, char c) {
    return String.valueOf(c).repeat(n);
  }

  // ---- HTTP ----------------------------------------------------------------------------------

  record Resp(int status, byte[] bytes, HttpHeaders headers) {
    String body() {
      return new String(bytes, StandardCharsets.UTF_8);
    }

    JsonNode json() {
      return parse(body());
    }

    String code() {
      return json().path("code").asText("");
    }

    List<String> fieldCodes() {
      return texts(json().path("fieldErrors"), "code");
    }

    Resp is(int expected) {
      assertThat(status).as("status; body %s", body().length() > 400 ? body().substring(0, 400) : body()).isEqualTo(expected);
      return this;
    }

    Resp is(int expected, String errorCode) {
      is(expected);
      assertThat(code()).as("error code; body %s", body()).isEqualTo(errorCode);
      return this;
    }

    String id() {
      return json().path("id").asText();
    }
  }

  Resp send(String method, String path, String token, Object body, Map<String, String> headers) {
    String full = path.startsWith("/health") || path.startsWith("/api/") ? path : "/api/v1" + path;
    HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + full)).timeout(Duration.ofSeconds(120));
    if (token != null) {
      b.header("Authorization", "Bearer " + token);
    }
    headers.forEach(b::header);
    if (body == null) {
      b.method(method, HttpRequest.BodyPublishers.noBody());
    } else {
      if (headers.keySet().stream().noneMatch(k -> k.equalsIgnoreCase("Content-Type"))) {
        b.header("Content-Type", "application/json");
      }
      String text = body instanceof String s ? s : write(body);
      b.method(method, HttpRequest.BodyPublishers.ofString(text));
    }
    try {
      HttpResponse<byte[]> r = CLIENT.send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
      return new Resp(r.statusCode(), r.body(), r.headers());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  static String write(Object o) {
    try {
      return JSON.writeValueAsString(o);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  /** A person with an emulator account and a token. */
  final class U {
    final String email;
    final String name;
    final String uid;
    String token;

    U(String email, String name, String uid) {
      this.email = email;
      this.name = name;
      this.uid = uid;
      this.token = signIn(email);
    }

    Resp get(String p) {
      return send("GET", p, token, null, Map.of());
    }

    Resp post(String p, Object body) {
      return send("POST", p, token, body, Map.of());
    }

    Resp post(String p) {
      return send("POST", p, token, null, Map.of());
    }

    Resp put(String p, Object body) {
      return send("PUT", p, token, body, Map.of());
    }

    Resp patch(String p, Object body) {
      return send("PATCH", p, token, body, Map.of());
    }

    Resp delete(String p) {
      return send("DELETE", p, token, null, Map.of());
    }

    U refresh() {
      token = signIn(email);
      return this;
    }

    String userId() {
      return get("/me").is(200).json().path("id").asText();
    }
  }

  // ---- emulator ------------------------------------------------------------------------------

  static JsonNode emulator(String method, String path, Object body, boolean admin) {
    HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:9099" + path));
    if (admin) {
      b.header("Authorization", "Bearer owner");
    }
    if (body != null) {
      b.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(write(body)));
    } else {
      b.method(method, HttpRequest.BodyPublishers.noBody());
    }
    try {
      return parse(CLIENT.send(b.build(), HttpResponse.BodyHandlers.ofString()).body());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  static String signIn(String email) {
    JsonNode a =
        emulator(
            "POST",
            "/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=fake-api-key",
            obj("email", email, "password", PASSWORD, "returnSecureToken", true),
            false);
    assertThat(a.has("idToken")).as("sign-in %s", a).isTrue();
    return a.get("idToken").asText();
  }

  U account(String name, String email, boolean verified) {
    JsonNode a =
        emulator(
            "POST",
            "/identitytoolkit.googleapis.com/v1/projects/" + PROJECT + "/accounts",
            obj("email", email, "password", PASSWORD, "displayName", name, "emailVerified", verified),
            true);
    return new U(email, name, a.path("localId").asText());
  }

  U user(String name, String email) {
    U u = account(name, email, true);
    u.get("/me").is(200);
    return u;
  }

  static void verify(String uid) {
    emulator("POST", "/identitytoolkit.googleapis.com/v1/projects/" + PROJECT + "/accounts:update", obj("localId", uid, "emailVerified", true), true);
  }

  static void setValidSince(String uid, Instant since) {
    emulator(
        "POST",
        "/identitytoolkit.googleapis.com/v1/projects/" + PROJECT + "/accounts:update",
        obj("localId", uid, "validSince", String.valueOf(since.getEpochSecond())),
        true);
  }

  static boolean accountExists(String email) {
    JsonNode a = emulator("POST", "/identitytoolkit.googleapis.com/v1/projects/" + PROJECT + "/accounts:lookup", obj("email", arr(email)), true);
    return a.path("users").isArray() && !a.path("users").isEmpty();
  }

  static String googleToken(String email, String name) {
    ObjectNode claims = obj("sub", "g-" + email, "email", email, "email_verified", true);
    if (name != null) {
      claims.put("name", name);
    }
    JsonNode a =
        emulator(
            "POST",
            "/identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=fake-api-key",
            obj("postBody", "id_token=" + enc(write(claims)) + "&providerId=google.com", "requestUri", "http://localhost",
                "returnIdpCredential", true, "returnSecureToken", true),
            false);
    return a.path("idToken").asText();
  }

  static String forged(String audience, String uid, String email, Instant iat, Instant exp) {
    var enc = java.util.Base64.getUrlEncoder().withoutPadding();
    String header = write(obj("alg", "none", "typ", "JWT"));
    String claims =
        write(
            obj("iss", "https://securetoken.google.com/" + audience, "aud", audience, "sub", uid, "user_id", uid,
                "auth_time", iat.getEpochSecond(), "iat", iat.getEpochSecond(), "exp", exp.getEpochSecond(),
                "email", email, "email_verified", true, "firebase", obj("sign_in_provider", "password")));
    return enc.encodeToString(header.getBytes(StandardCharsets.UTF_8)) + "." + enc.encodeToString(claims.getBytes(StandardCharsets.UTF_8)) + ".";
  }

  // ---- product helpers -----------------------------------------------------------------------

  record G(String id, String name) {
    String p() {
      return "/groups/" + id;
    }

    String contacts() {
      return p() + "/contacts";
    }

    String contact(String c) {
      return contacts() + "/" + c;
    }
  }

  G group(U owner, String name) {
    Resp r = owner.post("/groups", obj("name", name)).is(201);
    return new G(r.id(), r.json().path("name").asText());
  }

  void join(G g, U owner, U u, String role) {
    owner.post(g.p() + "/invitations", obj("email", u.email, "role", role)).is(201);
    assertThat(texts(u.get("/groups").is(200).json(), "id")).contains(g.id());
  }

  U member(G g, U owner, String name, String email, String role) {
    U u = user(name, email);
    join(g, owner, u, role);
    return u;
  }

  String role(U u, G g) {
    for (JsonNode n : list(u.get("/groups").is(200).json())) {
      if (n.path("id").asText().equals(g.id())) {
        return n.path("role").asText();
      }
    }
    return null;
  }

  String contact(U u, G g, Object... kv) {
    return u.post(g.contacts(), obj(kv)).is(201).id();
  }

  JsonNode read(U u, G g, String c) {
    return u.get(g.contact(c)).is(200).json();
  }

  static ObjectNode editBody(JsonNode current, Object... changes) {
    ObjectNode b = JSON.createObjectNode();
    for (String f : FIELDS) {
      if (current.has(f)) {
        b.set(f, current.get(f));
      }
    }
    for (String k : List.of("tags", "attributes", "version")) {
      if (current.has(k)) {
        b.set(k, current.get(k));
      }
    }
    obj(changes).properties().forEach(e -> b.set(e.getKey(), e.getValue()));
    return b;
  }

  Resp save(U u, G g, JsonNode current, Object... changes) {
    return u.put(g.contact(current.path("id").asText()), editBody(current, changes));
  }

  JsonNode edit(U u, G g, String c, Object... changes) {
    return save(u, g, read(u, g, c), changes).is(200).json();
  }

  JsonNode page(U u, G g, String q) {
    return u.get(g.contacts() + (q.isEmpty() ? "" : "?" + q)).is(200).json();
  }

  int total(U u, G g, String q) {
    return page(u, g, q).path("total").asInt();
  }

  List<String> ids(U u, G g, String q) {
    return texts(page(u, g, q).path("items"), "id");
  }

  List<JsonNode> history(U u, G g, String c) {
    return list(u.get(g.contact(c) + "/history").is(200).json().path("items"));
  }

  List<JsonNode> activity(U u, G g, String q) {
    return list(u.get(g.p() + "/activity" + (q.isEmpty() ? "" : "?" + q)).is(200).json().path("items"));
  }

  static List<String> actions(List<JsonNode> entries) {
    return entries.stream().map(e -> e.path("action").asText()).toList();
  }

  List<JsonNode> tags(U u, G g) {
    return list(u.get(g.p() + "/tags").is(200).json());
  }

  String tagId(U u, G g, String name) {
    return tags(u, g).stream().filter(t -> t.path("name").asText().equals(name)).findFirst().orElseThrow().path("id").asText();
  }

  void trash(U u, G g, String c) {
    u.post(g.contact(c) + "/trash").is(204);
  }

  String memberId(U u, G g, String email) {
    return list(u.get(g.p() + "/members").is(200).json()).stream()
        .filter(m -> m.path("email").asText().equals(email))
        .findFirst()
        .orElseThrow()
        .path("userId")
        .asText();
  }

  Resp export(U u, G g, String lang, Object selection) {
    return u.post(g.p() + "/exports", obj("selection", selection, "lang", lang));
  }

  // ---- database fixtures and checks (the schema is the persistence contract) -----------------

  static Connection owner() throws Exception {
    return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
  }

  static long count(String table, String column, String id) {
    try (Connection c = owner();
        PreparedStatement s = c.prepareStatement("select count(*) from \"" + table + "\" where \"" + column + "\" = ?::uuid")) {
      s.setString(1, id);
      try (ResultSet rs = s.executeQuery()) {
        rs.next();
        return rs.getLong(1);
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  static void bulk(String groupId, String userId, int n, String lastName) {
    String sql =
        "insert into contact (id, group_id, first_name, last_name, company, first_name_norm, last_name_norm, company_norm,"
            + " email_norm, search_norm, created_by) select gen_random_uuid(), ?::uuid, 'Oseba' || g, ?, 'Masa d.o.o.',"
            + " 'oseba' || g, lower(?), 'masa d.o.o.', null, 'oseba' || g || ' ' || lower(?) || ' masa d.o.o.', ?::uuid"
            + " from generate_series(1, ?) g";
    try (Connection c = owner();
        PreparedStatement s = c.prepareStatement(sql)) {
      s.setString(1, groupId);
      s.setString(2, lastName);
      s.setString(3, lastName);
      s.setString(4, lastName);
      s.setString(5, userId);
      s.setInt(6, n);
      s.executeUpdate();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  // ---- xlsx ----------------------------------------------------------------------------------

  record Sheet1(int sheets, List<String> header, List<List<String>> rows, int nonText) {
    List<String> col(String name) {
      int i = header.indexOf(name);
      return rows.stream().map(r -> i < r.size() ? r.get(i) : "").toList();
    }
  }

  static Sheet1 xlsx(byte[] bytes) {
    try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
      Sheet sheet = wb.getSheetAt(0);
      int width = 0;
      for (Row r : sheet) {
        width = Math.max(width, r.getLastCellNum());
      }
      List<List<String>> all = new ArrayList<>();
      int nonText = 0;
      for (int r = 0; r <= sheet.getLastRowNum(); r++) {
        Row row = sheet.getRow(r);
        List<String> vals = new ArrayList<>();
        for (int c = 0; c < width; c++) {
          Cell cell = row == null ? null : row.getCell(c);
          if (cell == null || cell.getCellType() == CellType.BLANK) {
            vals.add("");
          } else if (cell.getCellType() == CellType.STRING) {
            vals.add(cell.getStringCellValue());
          } else {
            nonText++;
            vals.add(cell.toString());
          }
        }
        all.add(vals);
      }
      return new Sheet1(wb.getNumberOfSheets(), all.isEmpty() ? List.of() : all.get(0), all.size() > 1 ? all.subList(1, all.size()) : List.of(), nonText);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  // ---- time and concurrency ------------------------------------------------------------------

  static void sleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  static void awaitTrue(Callable<Boolean> condition) {
    Awaitility.await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(250)).until(condition);
  }

  @SafeVarargs
  static <T> List<T> atOnce(Callable<T>... calls) throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(calls.length);
    try {
      CountDownLatch go = new CountDownLatch(1);
      List<Future<T>> fs = new ArrayList<>();
      for (Callable<T> c : calls) {
        fs.add(pool.submit(() -> {
          go.await();
          return c.call();
        }));
      }
      go.countDown();
      List<T> out = new ArrayList<>();
      for (Future<T> f : fs) {
        out.add(f.get());
      }
      return out;
    } finally {
      pool.shutdownNow();
    }
  }

  static String uuid() {
    return UUID.randomUUID().toString();
  }
}
