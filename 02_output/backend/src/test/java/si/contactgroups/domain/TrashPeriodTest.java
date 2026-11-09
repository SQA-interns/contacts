package si.contactgroups.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** BR-04, R-012-03, R-012-05: the 30 days of Trash. */
class TrashPeriodTest {

  private static final Instant DELETED = Instant.parse("2026-11-01T10:00:00Z");

  @Test
  void aContactDeletedJustNowHas30DaysLeft() {
    assertThat(TrashPeriod.daysLeft(DELETED, DELETED)).isEqualTo(30);
  }

  @Test
  void aPartOfADayCountsAsADayLeft() {
    Instant now = DELETED.plus(Duration.ofDays(29).plusHours(23));
    assertThat(TrashPeriod.daysLeft(DELETED, now)).isEqualTo(1);
  }

  @Test
  void afterOneDayAndAMinute29DaysAreLeft() {
    Instant now = DELETED.plus(Duration.ofDays(1).plusMinutes(1));
    assertThat(TrashPeriod.daysLeft(DELETED, now)).isEqualTo(29);
  }

  @Test
  void after30DaysNoneAreLeftAndItIsNotRestorable() {
    Instant now = DELETED.plus(Duration.ofDays(30));
    assertThat(TrashPeriod.daysLeft(DELETED, now)).isZero();
    assertThat(TrashPeriod.isRestorable(DELETED, now)).isFalse();
  }

  @Test
  void aMinuteBefore30DaysItIsRestorable() {
    Instant now = DELETED.plus(Duration.ofDays(30)).minusSeconds(60);
    assertThat(TrashPeriod.isRestorable(DELETED, now)).isTrue();
  }

  @Test
  void theCutoffIs30DaysBeforeNow() {
    assertThat(TrashPeriod.cutoff(DELETED)).isEqualTo(DELETED.minus(Duration.ofDays(30)));
  }
}
