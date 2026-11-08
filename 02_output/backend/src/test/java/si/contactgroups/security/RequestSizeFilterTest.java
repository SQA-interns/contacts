package si.contactgroups.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.unit.DataSize;

/** R-018-11: a body without a {@code Content-Length} header is counted while it is read. */
class RequestSizeFilterTest {

  private final RequestSizeFilter filter = new RequestSizeFilter(DataSize.ofBytes(10));

  /** A request whose length is not announced, as with chunked transfer. */
  private static MockHttpServletRequest unannounced(String body) {
    MockHttpServletRequest request =
        new MockHttpServletRequest("POST", "/api/v1/groups") {
          @Override
          public long getContentLengthLong() {
            return -1;
          }
        };
    request.setContent(body.getBytes(StandardCharsets.UTF_8));
    return request;
  }

  private static String readAll(HttpServletRequest request) throws IOException {
    return new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
  }

  @Test
  void a_body_over_the_limit_without_announced_length_fails_while_it_is_read() {
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertThatThrownBy(
            () ->
                filter.doFilter(
                    unannounced("12345678901"),
                    response,
                    (request, ignored) -> readAll((HttpServletRequest) request)))
        .isInstanceOf(RequestTooLargeException.class);
  }

  @Test
  void a_body_of_exactly_the_limit_without_announced_length_is_read_whole() throws Exception {
    StringBuilder seen = new StringBuilder();

    filter.doFilter(
        unannounced("1234567890"),
        new MockHttpServletResponse(),
        (request, ignored) -> seen.append(readAll((HttpServletRequest) request)));

    assertThat(seen).hasToString("1234567890");
  }
}
