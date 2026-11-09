package si.contactgroups.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Calendar days of the activity filter, both included, in Europe/Ljubljana (R-017-04). Either day
 * may be missing.
 */
public record ActivityRange(LocalDate from, LocalDate to) {

  private static final ZoneId ZONE = ZoneId.of("Europe/Ljubljana");

  /** Start of the first day as an instant, or {@code null} without a start day. */
  public Instant fromInclusive() {
    return from == null ? null : from.atStartOfDay(ZONE).toInstant();
  }

  /** Start of the day after the last day, or {@code null} without an end day. */
  public Instant toExclusive() {
    return to == null ? null : to.plusDays(1).atStartOfDay(ZONE).toInstant();
  }

  /** True when the start day is after the end day (R-017-06). */
  public boolean isInverted() {
    return from != null && to != null && from.isAfter(to);
  }
}
