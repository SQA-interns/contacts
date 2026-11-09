package si.contactgroups.config;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Firebase Admin SDK with the project id and, in production, the service account of
 * GOOGLE_APPLICATION_CREDENTIALS: tokens are checked against Firebase's public keys, but reading
 * and deleting an account (R-021-01, R-021-05) are Admin API calls (D-69). When
 * FIREBASE_AUTH_EMULATOR_HOST is set in the environment the SDK talks to the emulator.
 *
 * <p>The SDK is given {@link NetHttpTransport} on purpose (F-26): its own default transport runs on
 * Apache HttpClient 5, which already inflates a gzip response, and the Google HTTP client then
 * inflates it again, so the key fetch of every real token failed with "Not in GZIP format".
 */
@Configuration
public class FirebaseConfig {

  @Bean
  public FirebaseApp firebaseApp(AppProperties properties, Environment environment) {
    FirebaseOptions options =
        options(properties.firebaseProjectId(), FirebaseCredentials.load(environment));
    return FirebaseApp.initializeApp(options, "contact-groups");
  }

  static FirebaseOptions options(String projectId, GoogleCredentials credentials) {
    return FirebaseOptions.builder()
        .setProjectId(projectId)
        .setCredentials(credentials)
        .setHttpTransport(new NetHttpTransport())
        .build();
  }
}
