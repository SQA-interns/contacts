package si.contactgroups.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.env.Environment;

/**
 * The credentials of the Firebase Admin SDK (ADR-04, D-69): the service-account file named by
 * GOOGLE_APPLICATION_CREDENTIALS, or none for the emulator and tests.
 */
final class FirebaseCredentials {

  static final String VARIABLE = "GOOGLE_APPLICATION_CREDENTIALS";

  private FirebaseCredentials() {}

  /**
   * The service account of the file, or empty credentials when the variable is unset or blank.
   *
   * @throws IllegalStateException when the file is missing or is no service-account key; the
   *     message never holds the content of the file
   */
  static GoogleCredentials load(Environment environment) {
    String path = environment.getProperty(VARIABLE, "").strip();
    if (path.isEmpty()) {
      return GoogleCredentials.newBuilder().build();
    }
    try (InputStream in = Files.newInputStream(Path.of(path))) {
      return ServiceAccountCredentials.fromStream(in);
    } catch (IOException | RuntimeException e) {
      // The cause is dropped on purpose: its message may quote the key file.
      throw new IllegalStateException(
          VARIABLE + " does not name a readable Firebase service-account file");
    }
  }
}
