package si.contactgroups.domain;

import java.time.Duration;
import java.time.Instant;

/** The 30 days a Contact stays in Trash (BR-04): days left, restorable or not. */
public final class TrashPeriod {

  /** The Trash period of BR-04. */
  public static final Duration PERIOD = Duration.ofDays(30);

  private static final long DAYS = 30;

  private TrashPeriod() {}

  /** Whole days left, a started day counting as one; 30 right after deletion, 0 from day 30 on. */
  public static int daysLeft(Instant deletedAt, Instant now) {
    Duration left = Duration.between(now, deletedAt.plus(PERIOD));
    if (left.isNegative() || left.isZero()) {
      return 0;
    }
    long days =
        (left.toSeconds() + Duration.ofDays(1).toSeconds() - 1) / Duration.ofDays(1).toSeconds();
    return (int) Math.min(DAYS, days);
  }

  /** Whether a Contact deleted at {@code deletedAt} has been in Trash for less than 30 days. */
  public static boolean isRestorable(Instant deletedAt, Instant now) {
    return deletedAt.isAfter(cutoff(now));
  }

  /** A Contact deleted at or before this instant is past the period. */
  public static Instant cutoff(Instant now) {
    return now.minus(PERIOD);
  }
}
