package si.contactgroups.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import si.contactgroups.domain.User;
import si.contactgroups.support.WebTestSupport;

/**
 * US-018 T-10: the filters of the security chain work together on a real request: the rate limit
 * (R-018-10), the log (R-018-13), CORS (R-018-16) and the API headers (SB-10).
 */
@ExtendWith(OutputCaptureExtension.class)
class SecurityChainIT extends WebTestSupport {

  private static final String OWN_ORIGIN = "http://localhost:5173";

  @Test
  void r_018_10_the_101st_request_of_a_user_in_a_minute_is_answered_429() throws Exception {
    User ana = newUser("Ana Novak");
    User boris = newUser("Boris Kos");

    for (int i = 0; i < 100; i++) {
      mvc.perform(get("/api/v1/groups").with(as(ana))).andExpect(status().isOk());
    }

    mvc.perform(get("/api/v1/groups").with(as(ana)))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value("rate.limited"))
        .andExpect(header().exists("Retry-After"));
    mvc.perform(get("/api/v1/groups").with(as(boris))).andExpect(status().isOk());
  }

  @Test
  void r_018_13_a_failed_request_is_logged_once_without_the_token(CapturedOutput output)
      throws Exception {
    mvc.perform(
            get("/api/v1/groups")
                .header("Authorization", "Bearer not-a-valid-token-0123456789")
                .queryParam("q", "Zaupna"))
        .andExpect(status().isUnauthorized());

    assertThat(output.getAll().lines().filter(l -> l.contains("answered 401")))
        .singleElement()
        .asString()
        .doesNotContain("not-a-valid-token-0123456789")
        .doesNotContain("Zaupna");
    assertThat(output.getAll()).doesNotContain("not-a-valid-token-0123456789");
  }

  @Test
  void r_018_16_a_foreign_origin_gets_no_cors_permission_and_the_own_origin_does()
      throws Exception {
    User ana = newUser("Ana Novak");

    mvc.perform(
            options("/api/v1/groups")
                .header("Origin", "https://evil.example")
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    mvc.perform(get("/api/v1/groups").with(as(ana)).header("Origin", "https://evil.example"))
        .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    mvc.perform(
            options("/api/v1/groups")
                .header("Origin", OWN_ORIGIN)
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(header().string("Access-Control-Allow-Origin", OWN_ORIGIN));
  }

  @Test
  void sb_10_the_api_headers_are_set() throws Exception {
    User ana = newUser("Ana Novak");

    mvc.perform(get("/api/v1/me").with(as(ana)))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().string("X-Frame-Options", "DENY"))
        .andExpect(header().string("Referrer-Policy", "no-referrer"))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(
            header()
                .string(
                    "Content-Security-Policy",
                    org.hamcrest.Matchers.containsString("default-src 'none'")));
  }
}
