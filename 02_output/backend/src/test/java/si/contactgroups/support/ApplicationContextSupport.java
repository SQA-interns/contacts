package si.contactgroups.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The one application context of the integration tests outside {@code acceptance/}: the product
 * registers one named {@code FirebaseApp} per JVM (D-38e), so every such test class extends this
 * base and Spring reuses the cached context.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class ApplicationContextSupport {

  @DynamicPropertySource
  static void environment(DynamicPropertyRegistry registry) {
    registry.add("DB_URL", PostgresSupport::jdbcUrl);
    registry.add("DB_MIGRATION_USER", PostgresSupport::ownerUser);
    registry.add("DB_MIGRATION_PASSWORD", PostgresSupport::ownerPassword);
    registry.add("DB_APP_PASSWORD", () -> PostgresSupport.APP_PASSWORD);
    registry.add("FIREBASE_PROJECT_ID", () -> "demo-contact-groups");
    registry.add("CORS_ALLOWED_ORIGIN", () -> "http://localhost:5173");
  }
}
