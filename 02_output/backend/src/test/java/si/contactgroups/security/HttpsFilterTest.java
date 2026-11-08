package si.contactgroups.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** R-018-12: with {@code REQUIRE_HTTPS} a request that did not arrive over HTTPS is refused. */
class HttpsFilterTest {

  private static MockHttpServletRequest request(String path, boolean secure) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
    request.setSecure(secure);
    request.setScheme(secure ? "https" : "http");
    return request;
  }

  private static boolean passed(
      HttpsFilter filter, MockHttpServletRequest request, MockHttpServletResponse response)
      throws Exception {
    MockFilterChain chain = new MockFilterChain();
    filter.doFilter(request, response, chain);
    return chain.getRequest() != null;
  }

  @Test
  void r_018_12_a_plain_request_is_refused_with_403_https_required() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    boolean passed = passed(new HttpsFilter(true), request("/api/v1/groups", false), response);

    assertThat(passed).isFalse();
    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentAsString()).contains("\"code\":\"https.required\"");
  }

  @Test
  void r_018_12_a_request_over_https_passes() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertThat(passed(new HttpsFilter(true), request("/api/v1/groups", true), response)).isTrue();
  }

  @Test
  void r_018_12_the_health_check_stays_open_for_the_container_health_check() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertThat(passed(new HttpsFilter(true), request("/health", false), response)).isTrue();
  }

  @Test
  void r_018_12_without_the_setting_a_plain_request_passes() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertThat(passed(new HttpsFilter(false), request("/api/v1/groups", false), response)).isTrue();
  }
}
