package si.contactgroups;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;
import si.contactgroups.support.ApplicationContextSupport;

/** The assembled skeleton: SR-02, R-018-01, R-018-07, SB-10, R-018-09. */
class ApplicationIT extends ApplicationContextSupport {

  @Autowired private MockMvc mvc;
  @Autowired private UserDetailsService userDetailsService;

  @Test
  void thereIsNoLocalUserAndNoGeneratedPassword() {
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> userDetailsService.loadUserByUsername("user"))
        .isInstanceOf(UsernameNotFoundException.class);
  }

  @Test
  void healthIsPublicAndReturnsOnlyTheStatus() throws Exception {
    mvc.perform(get("/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.components").doesNotExist());
  }

  @Test
  void healthNamesNoOtherKeyNotEvenTheProbeGroups() throws Exception {
    String body = mvc.perform(get("/health")).andReturn().getResponse().getContentAsString();

    org.assertj.core.api.Assertions.assertThat(
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).fieldNames())
        .toIterable()
        .containsExactly("status");
  }

  @Test
  void theContainerHealthCheckCallsAnAddressThatAnswers() throws Exception {
    String dockerfile = java.nio.file.Files.readString(java.nio.file.Path.of("Dockerfile"));
    java.util.regex.Matcher url =
        java.util.regex.Pattern.compile("curl -fsS http://127\\.0\\.0\\.1:8080(\\S*)")
            .matcher(dockerfile);

    org.assertj.core.api.Assertions.assertThat(url.find()).isTrue();
    mvc.perform(get(url.group(1))).andExpect(status().isOk());
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
  void unknownPathWithAValidPrincipalIs404InTheErrorFormat() throws Exception {
    mvc.perform(get("/api/v1/nothing").with(user("ana")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("route.not_found"));
  }

  @Test
  void aMethodThatDoesNotExistIs405InTheErrorFormat() throws Exception {
    mvc.perform(delete("/api/v1/openapi.json").with(user("ana")))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.code").value("request.method_not_allowed"));
  }

  @Test
  void theOpenApiDescriptionIsPublishedToSignedInUsersOnly() throws Exception {
    mvc.perform(get("/api/v1/openapi.json")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/openapi.json").with(user("ana")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.openapi").exists());
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
