package si.contactgroups.acceptance.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

/**
 * The Firebase Authentication emulator (environments.md, "test"): one container per test JVM on
 * the fixed port 9099, because the Admin SDK of the backend reads {@code
 * FIREBASE_AUTH_EMULATOR_HOST=localhost:9099} from the environment (pom.xml, failsafe). Accounts
 * are created and changed through the emulator's REST interface; all values are test values that
 * exist only in the emulator.
 */
public final class Emulator {

  public static final String PROJECT_ID = "demo-contact-groups";
  public static final int PORT = 9099;
  private static final String BASE = "http://localhost:" + PORT;
  private static final HttpClient HTTP = HttpClient.newHttpClient();
  private static final GenericContainer<?> CONTAINER = start();

  private Emulator() {}

  @SuppressWarnings("resource")
  private static GenericContainer<?> start() {
    RuntimeException last = null;
    // the previous test JVM may still hold the fixed port for a moment
    for (int attempt = 0; attempt < 6; attempt++) {
      try {
        GenericContainer<?> container = newContainer();
        container.start();
        Runtime.getRuntime().addShutdownHook(new Thread(container::stop));
        return container;
      } catch (RuntimeException e) {
        last = e;
        try {
          Thread.sleep(5000);
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          throw e;
        }
      }
    }
    throw last;
  }

  @SuppressWarnings("resource")
  private static GenericContainer<?> newContainer() {
    GenericContainer<?> container =
        new GenericContainer<>(
                new ImageFromDockerfile("contact-groups-emulator-test", false)
                    .withFileFromPath(".", Path.of("..", "emulator")))
            .withExposedPorts(PORT)
            .withCreateContainerCmdModifier(
                cmd ->
                    cmd.getHostConfig()
                        .withPortBindings(
                            com.github.dockerjava.api.model.PortBinding.parse(PORT + ":" + PORT)))
            .waitingFor(Wait.forLogMessage(".*All emulators ready.*", 1))
            .withStartupTimeout(Duration.ofMinutes(3));
    return container;
  }

  /** Makes sure the container runs (class initialisation starts it). */
  public static void ensureStarted() {
    assertThat(CONTAINER.isRunning()).isTrue();
  }

  /** Removes every account (between tests). */
  public static void clear() {
    send("DELETE", "/emulator/v1/projects/" + PROJECT_ID + "/accounts", null, true);
  }

  /** Creates an account with a password; returns the Firebase uid. */
  public static String createAccount(
      String email, String password, String displayName, boolean emailVerified) {
    var body = Json.obj("email", email, "password", password, "emailVerified", emailVerified);
    if (displayName != null) {
      body.put("displayName", displayName);
    }
    JsonNode answer =
        send("POST", "/identitytoolkit.googleapis.com/v1/projects/" + PROJECT_ID + "/accounts", body, true);
    assertThat(answer.has("localId")).as("emulator account created: %s", answer).isTrue();
    return answer.get("localId").asText();
  }

  /** Signs in with e-mail and password and returns the ID token. */
  public static String signIn(String email, String password) {
    JsonNode answer =
        send(
            "POST",
            "/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=fake-api-key",
            Json.obj("email", email, "password", password, "returnSecureToken", true),
            false);
    assertThat(answer.has("idToken")).as("emulator sign-in: %s", answer).isTrue();
    return answer.get("idToken").asText();
  }

  /**
   * "Sign in with Google" through the emulator's fake identity provider; returns the ID token. A
   * {@code null} name gives a Google profile without a name.
   */
  public static String signInWithGoogle(String email, String name) {
    var claims = Json.obj("sub", "google-" + email, "email", email, "email_verified", true);
    if (name != null) {
      claims.put("name", name);
    }
    String postBody =
        "id_token="
            + URLEncoder.encode(Json.write(claims), StandardCharsets.UTF_8)
            + "&providerId=google.com";
    JsonNode answer =
        send(
            "POST",
            "/identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=fake-api-key",
            Json.obj(
                "postBody",
                postBody,
                "requestUri",
                "http://localhost",
                "returnIdpCredential",
                true,
                "returnSecureToken",
                true),
            false);
    assertThat(answer.has("idToken")).as("emulator Google sign-in: %s", answer).isTrue();
    return answer.get("idToken").asText();
  }

  public static void setEmailVerified(String uid, boolean verified) {
    send(
        "POST",
        "/identitytoolkit.googleapis.com/v1/projects/" + PROJECT_ID + "/accounts:update",
        Json.obj("localId", uid, "emailVerified", verified),
        true);
  }

  public static void setDisplayName(String uid, String displayName) {
    send(
        "POST",
        "/identitytoolkit.googleapis.com/v1/projects/" + PROJECT_ID + "/accounts:update",
        Json.obj("localId", uid, "displayName", displayName),
        true);
  }

  /** Deletes an account directly in the emulator (not through the product). */
  public static void deleteAccount(String uid) {
    send(
        "POST",
        "/identitytoolkit.googleapis.com/v1/projects/" + PROJECT_ID + "/accounts:delete",
        Json.obj("localId", uid),
        true);
  }

  /** True if the emulator has an account with this e-mail address. */
  public static boolean accountExists(String email) {
    JsonNode answer =
        send(
            "POST",
            "/identitytoolkit.googleapis.com/v1/projects/" + PROJECT_ID + "/accounts:lookup",
            Json.obj("email", Json.arr(email)),
            true);
    return answer != null && answer.path("users").isArray() && !answer.path("users").isEmpty();
  }

  /** The e-mails (verification, reset, …) the emulator recorded as sent, as {@code email} values. */
  public static List<String> sentEmails() {
    JsonNode answer = send("GET", "/emulator/v1/projects/" + PROJECT_ID + "/oobCodes", null, false);
    return Json.field(answer.path("oobCodes"), "email");
  }

  /**
   * Makes every token issued before {@code instant} invalid for this account (the emulator then
   * accepts older tokens only if they are newer than this).
   */
  public static void setValidSince(String uid, Instant instant) {
    send(
        "POST",
        "/identitytoolkit.googleapis.com/v1/projects/" + PROJECT_ID + "/accounts:update",
        Json.obj("localId", uid, "validSince", String.valueOf(instant.getEpochSecond())),
        true);
  }

  /**
   * An unsigned token in the emulator's format with chosen claims, for the cases that the emulator
   * cannot issue (expired, other project). The account must be valid since before {@code
   * issuedAt} ({@link #setValidSince}).
   */
  public static String forgedToken(
      String audience, String uid, String email, Instant issuedAt, Instant expiresAt) {
    long exp = expiresAt.getEpochSecond();
    long iat = issuedAt.getEpochSecond();
    var header = Json.obj("alg", "none", "typ", "JWT");
    var claims =
        Json.obj(
            "iss",
            "https://securetoken.google.com/" + audience,
            "aud",
            audience,
            "sub",
            uid,
            "user_id",
            uid,
            "auth_time",
            iat,
            "iat",
            iat,
            "exp",
            exp,
            "email",
            email,
            "email_verified",
            true,
            "firebase",
            Json.obj("sign_in_provider", "password"));
    Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
    return enc.encodeToString(Json.write(header).getBytes(StandardCharsets.UTF_8))
        + "."
        + enc.encodeToString(Json.write(claims).getBytes(StandardCharsets.UTF_8))
        + ".";
  }

  private static JsonNode send(String method, String path, Object body, boolean admin) {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(URI.create(BASE + path)).timeout(Duration.ofSeconds(30));
    if (admin) {
      request.header("Authorization", "Bearer owner");
    }
    if (body != null) {
      request.header("Content-Type", "application/json");
      request.method(method, HttpRequest.BodyPublishers.ofString(Json.write(body)));
    } else {
      request.method(method, HttpRequest.BodyPublishers.noBody());
    }
    try {
      HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
      return Json.parse(response.body());
    } catch (java.io.IOException e) {
      throw new java.io.UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }
}
