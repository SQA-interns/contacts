package si.contactgroups.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** R-018-13: one line per failed request, without Contact data and without tokens. */
@ExtendWith(OutputCaptureExtension.class)
class SecurityLoggerTest {

  private final SecurityLogger logger = new SecurityLogger();

  private void answered(int status, CapturedOutput output) throws Exception {
    MockHttpServletRequest request =
        new MockHttpServletRequest("GET", "/api/v1/groups/g-1/contacts");
    request.setQueryString("q=Zaupna");
    request.addHeader("Authorization", "Bearer secret-token-value");
    request.setContent("{\"lastName\":\"Zaupna\"}".getBytes());
    logger.doFilter(
        request,
        new MockHttpServletResponse(),
        new MockFilterChain() {
          @Override
          public void doFilter(
              jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
            ((MockHttpServletResponse) res).setStatus(status);
          }
        });
  }

  private static List<String> linesWith(CapturedOutput output, String text) {
    return output.getAll().lines().filter(l -> l.contains(text)).toList();
  }

  @Test
  void ac_018_15_each_of_401_403_404_and_429_gives_one_line_with_status_method_and_path(
      CapturedOutput output) throws Exception {
    for (int status : new int[] {401, 403, 404, 429}) {
      answered(status, output);
    }

    for (String status : List.of("401", "403", "404", "429")) {
      assertThat(linesWith(output, "answered " + status))
          .singleElement()
          .asString()
          .contains("GET")
          .contains("/api/v1/groups/g-1/contacts");
    }
  }

  @Test
  void ac_018_15_a_line_holds_no_query_string_body_or_token(CapturedOutput output)
      throws Exception {
    answered(404, output);

    assertThat(output.getAll()).doesNotContain("Zaupna").doesNotContain("secret-token-value");
  }

  @Test
  void a_successful_request_gives_no_line(CapturedOutput output) throws Exception {
    answered(200, output);
    answered(201, output);

    assertThat(linesWith(output, "answered")).isEmpty();
  }
}
