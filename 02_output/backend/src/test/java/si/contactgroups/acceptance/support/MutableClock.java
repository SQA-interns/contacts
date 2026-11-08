package si.contactgroups.acceptance.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * The clock of the acceptance tests: it ticks like the system clock, shifted by an offset that a
 * test sets. Time therefore moves forward between two requests (History stays ordered) and a test
 * can still say "this happens 30 days later" (ADR-09: the product takes every time from the
 * injected {@link Clock}).
 */
public final class MutableClock extends Clock {

  private volatile Duration offset = Duration.ZERO;

  @Override
  public ZoneId getZone() {
    return ZoneOffset.UTC;
  }

  @Override
  public Clock withZone(ZoneId zone) {
    return Clock.offset(Clock.system(zone), offset);
  }

  @Override
  public Instant instant() {
    return Instant.now().plus(offset);
  }

  /** Back to the real time. */
  public void reset() {
    offset = Duration.ZERO;
  }

  /** From now on the clock reads {@code now} and then ticks on. */
  public void setNow(Instant now) {
    offset = Duration.between(Instant.now(), now);
  }

  public void advance(Duration duration) {
    offset = offset.plus(duration);
  }
}
