package si.contactgroups.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.TimeMeter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * At most N requests per minute for each User; every authenticated request counts, errors too
 * (R-018-10, SR-08, ADR-11). The tokens of one User's bucket are refilled once a minute. The
 * buckets live in memory: the product runs one backend instance.
 */
public class RateLimitFilter extends OncePerRequestFilter {

  private final int perMinute;
  private final TimeMeter time;
  private final Map<UUID, Bucket> buckets = new ConcurrentHashMap<>();

  public RateLimitFilter(Clock clock, int perMinute) {
    this.perMinute = perMinute;
    this.time = new ClockTimeMeter(clock);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
      ConsumptionProbe probe =
          buckets.computeIfAbsent(user.userId(), id -> newBucket()).tryConsumeAndReturnRemaining(1);
      if (!probe.isConsumed()) {
        long seconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
        response.setHeader("Retry-After", Long.toString(seconds));
        ErrorWriter.write(
            response,
            HttpStatus.TOO_MANY_REQUESTS.value(),
            "rate.limited",
            "Too many requests, try again later");
        return;
      }
    }
    chain.doFilter(request, response);
  }

  private Bucket newBucket() {
    return Bucket.builder()
        .addLimit(
            Bandwidth.builder()
                .capacity(perMinute)
                .refillIntervally(perMinute, Duration.ofMinutes(1))
                .build())
        .withCustomTimePrecision(time)
        .build();
  }

  /** The application clock as the time source of the buckets, so that tests can move time. */
  private record ClockTimeMeter(Clock clock) implements TimeMeter {

    @Override
    public long currentTimeNanos() {
      return TimeUnit.MILLISECONDS.toNanos(clock.millis());
    }

    @Override
    public boolean isWallClockBased() {
      return true;
    }
  }
}
