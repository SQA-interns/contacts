package si.contactgroups;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import si.contactgroups.support.PostgresSupport;

/** The assembled skeleton: SR-02, R-018-01, R-018-07, SB-10, R-018-09. */
@SpringBootTest
@AutoConfigureMockMvc
class ApplicationIT {

  @DynamicPropertySource
  static void environment(DynamicPropertyRegistry registry) {
    registry.add("DB_URL", PostgresSupport::jdbcUrl);
    registry.add("DB_MIGRATION_USER", PostgresSupport::ownerUser);
    registry.add("DB_MIGRATION_PASSWORD", PostgresSupport::ownerPassword);
    registry.add("DB_APP_PASSWORD", () -> PostgresSupport.APP_PASSWORD);
    registry.add("FIREBASE_PROJECT_ID", () -> "demo-contact-groups");
    registry.add("CORS_ALLOWED_ORIGIN", () -> "http://localhost:5173");
  }

  @Autowired private MockMvc mvc;

  @Test
  void healthIsPublicAndReturnsOnlyTheStatus() throws Exception {
    mvc.perform(get("/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.components").doesNotExist());
  }

  @Test
  void anyOtherPathWithoutTokenIs401() throws Exception {
    mvc.perform(get("/api/v1/groups"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("auth.token_missing"));
  }

  @Test
  void anInvalidTokenIs401WithoutDetail() throws Exception {
    mvc.perform(get("/api/v1/groups").header("Authorization", "Bearer not-a-token"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("auth.token_invalid"))
        .andExpect(jsonPath("$.stackTrace").doesNotExist());
  }

  @Test
  void apiResponsesCarrySecurityHeaders() throws Exception {
    mvc.perform(get("/api/v1/groups"))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().string("X-Frame-Options", "DENY"))
        .andExpect(header().string("Referrer-Policy", "no-referrer"))
        .andExpect(
            header()
                .string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"));
  }

  @Test
  void corsAllowsOnlyTheFrontendOrigin() throws Exception {
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options(
                    "/api/v1/groups")
                .header("Origin", "http://evil.example")
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isForbidden());
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options(
                    "/api/v1/groups")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
  }
}
