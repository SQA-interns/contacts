package si.contactgroups.config;

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
 */
@Configuration
public class FirebaseConfig {

  @Bean
  public FirebaseApp firebaseApp(AppProperties properties, Environment environment) {
    FirebaseOptions options =
        FirebaseOptions.builder()
            .setProjectId(properties.firebaseProjectId())
            .setCredentials(FirebaseCredentials.load(environment))
            .build();
    return FirebaseApp.initializeApp(options, "contact-groups");
  }
}
