package si.contactgroups.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** R-018-10, EX-018-03, EX-018-09: 100 requests per User and minute (the configured number). */
class RateLimitFilterTest {

  private static final int LIMIT = 100;

  /** A clock that the test moves. */
  private static final class Movable extends Clock {
    private Instant now = Instant.parse("2026-11-08T10:00:00Z");

    void advance(Duration by) {
      now = now.plus(by);
    }

    @Override
    public java.time.ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(java.time.ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }

  private final Movable clock = new Movable();
  private final RateLimitFilter filter = new RateLimitFilter(clock, LIMIT);

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  private static void signInAs(UUID userId) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(userId, "x@acme.com", true), null, java.util.List.of()));
  }

  private MockHttpServletResponse request() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(
        new MockHttpServletRequest("GET", "/api/v1/groups"), response, new MockFilterChain());
    return response;
  }

  private void requests(int count) throws Exception {
    for (int i = 0; i < count; i++) {
      assertThat(request().getStatus()).isEqualTo(200);
    }
  }

  @Test
  void ex_018_03_exactly_100_requests_in_a_minute_are_all_accepted() throws Exception {
    signInAs(UUID.randomUUID());

    requests(LIMIT);
  }

  @Test
  void r_018_10_the_101st_request_is_answered_429_rate_limited_with_retry_after() throws Exception {
    signInAs(UUID.randomUUID());
    requests(LIMIT);

    MockHttpServletResponse limited = request();

    assertThat(limited.getStatus()).isEqualTo(429);
    assertThat(limited.getContentAsString()).contains("\"code\":\"rate.limited\"");
    assertThat(Long.parseLong(limited.getHeader("Retry-After"))).isBetween(1L, 60L);
  }

  @Test
  void ac_018_12_after_the_minute_requests_work_again() throws Exception {
    signInAs(UUID.randomUUID());
    requests(LIMIT);
    assertThat(request().getStatus()).isEqualTo(429);

    clock.advance(Duration.ofSeconds(61));

    assertThat(request().getStatus()).isEqualTo(200);
  }

  @Test
  void ex_018_09_the_limit_is_per_user() throws Exception {
    signInAs(UUID.randomUUID());
    requests(LIMIT);

    signInAs(UUID.randomUUID());
    requests(LIMIT);
  }

  @Test
  void a_request_without_a_signed_in_user_is_not_counted() throws Exception {
    for (int i = 0; i < LIMIT + 5; i++) {
      assertThat(request().getStatus()).isEqualTo(200);
    }
  }
}
