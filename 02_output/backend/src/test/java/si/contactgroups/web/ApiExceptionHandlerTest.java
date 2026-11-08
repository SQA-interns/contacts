package si.contactgroups.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import si.contactgroups.error.ApiError;
import si.contactgroups.security.RequestTooLargeException;

/** R-018-09, R-018-11: how a failure while reading the body is answered. */
class ApiExceptionHandlerTest {

  private final ApiExceptionHandler handler = new ApiExceptionHandler();

  @Test
  void r_018_11_a_body_found_too_large_while_it_is_read_is_answered_413() {
    HttpMessageNotReadableException failure =
        new HttpMessageNotReadableException(
            "read failed",
            new RequestTooLargeException(),
            new ServletServerHttpRequest(new MockHttpServletRequest()));

    var answer = handler.malformed(failure);

    assertThat(answer.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    ApiError body = answer.getBody();
    assertThat(body.code()).isEqualTo("request.too_large");
    assertThat(body.message()).isNotBlank();
  }
}
