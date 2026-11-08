package si.contactgroups.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Firebase Admin SDK with the project id only: tokens are checked against Firebase's public keys,
 * so no service account is needed (ADR-04). When FIREBASE_AUTH_EMULATOR_HOST is set in the
 * environment the SDK talks to the emulator.
 */
@Configuration
public class FirebaseConfig {

  @Bean
  public FirebaseApp firebaseApp(AppProperties properties) {
    FirebaseOptions options =
        FirebaseOptions.builder()
            .setProjectId(properties.firebaseProjectId())
            .setCredentials(GoogleCredentials.newBuilder().build())
            .build();
    return FirebaseApp.initializeApp(options, "contact-groups");
  }
}
