package si.contactgroups;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import si.contactgroups.security.FirebaseTokenVerifier;
import si.contactgroups.security.InvalidTokenException;
import si.contactgroups.security.VerifiedToken;

/**
 * SR-13 / D-12: with {@code google-cloud-storage} and {@code google-cloud-firestore} excluded, the
 * Admin SDK still verifies ID tokens, here against the Firebase Auth emulator (the failsafe plug-in
 * sets FIREBASE_AUTH_EMULATOR_HOST=localhost:9099 and the container publishes that port).
 */
class FirebaseEmulatorIT {

  private static final int PORT = 9099;
  private static GenericContainer<?> emulator;
  private static FirebaseApp app;
  private static final HttpClient HTTP = HttpClient.newHttpClient();

  @BeforeAll
  @SuppressWarnings("resource")
  static void start() throws Exception {
    emulator =
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
    emulator.start();
    app =
        FirebaseApp.initializeApp(
            FirebaseOptions.builder()
                .setProjectId("demo-contact-groups")
                .setCredentials(GoogleCredentials.newBuilder().build())
                .build(),
            "emulator-it");
  }

  @AfterAll
  static void stop() {
    if (app != null) {
      app.delete();
    }
    if (emulator != null) {
      emulator.stop();
    }
  }

  private static String signUp(String email) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(
                URI.create(
                    "http://localhost:"
                        + PORT
                        + "/identitytoolkit.googleapis.com/v1/accounts:signUp?key=fake"))
            .header("Content-Type", "application/json")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    "{\"email\":\""
                        + email
                        + "\",\"password\":\"secret123\",\"returnSecureToken\":true}"))
            .build();
    String body = HTTP.send(request, HttpResponse.BodyHandlers.ofString()).body();
    Matcher m = Pattern.compile("\"idToken\":\"([^\"]+)\"").matcher(body);
    assertThat(m.find()).as(body).isTrue();
    return m.group(1);
  }

  @Test
  void aTokenIssuedByTheEmulatorIsVerified() throws Exception {
    FirebaseTokenVerifier verifier = new FirebaseTokenVerifier(app);
    VerifiedToken token = verifier.verify(signUp("ana@example.com"));
    assertThat(token.email()).isEqualTo("ana@example.com");
    assertThat(token.uid()).isNotBlank();
    assertThat(token.emailVerified()).isFalse();
  }

  @Test
  void garbageIsRejected() {
    FirebaseTokenVerifier verifier = new FirebaseTokenVerifier(app);
    assertThatThrownBy(() -> verifier.verify("garbage")).isInstanceOf(InvalidTokenException.class);
  }
}
