package si.contactgroups.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ActivityRangeTest {

  @Test
  void a_day_in_winter_time_runs_from_23_00_utc_to_23_00_utc() {
    ActivityRange r =
        new ActivityRange(LocalDate.parse("2026-11-07"), LocalDate.parse("2026-11-07"));

    assertThat(r.fromInclusive()).isEqualTo(Instant.parse("2026-11-06T23:00:00Z"));
    assertThat(r.toExclusive()).isEqualTo(Instant.parse("2026-11-07T23:00:00Z"));
  }

  @Test
  void a_day_in_summer_time_starts_at_22_00_utc() {
    ActivityRange r = new ActivityRange(LocalDate.parse("2026-07-01"), null);

    assertThat(r.fromInclusive()).isEqualTo(Instant.parse("2026-06-30T22:00:00Z"));
    assertThat(r.toExclusive()).isNull();
  }

  @Test
  void a_missing_start_has_no_lower_bound() {
    ActivityRange r = new ActivityRange(null, LocalDate.parse("2026-11-07"));

    assertThat(r.fromInclusive()).isNull();
    assertThat(r.toExclusive()).isEqualTo(Instant.parse("2026-11-07T23:00:00Z"));
  }

  @Test
  void start_after_end_is_invalid_and_equal_days_are_valid() {
    assertThat(
            new ActivityRange(LocalDate.parse("2026-11-10"), LocalDate.parse("2026-11-01"))
                .isInverted())
        .isTrue();
    assertThat(
            new ActivityRange(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-01"))
                .isInverted())
        .isFalse();
    assertThat(new ActivityRange(null, LocalDate.parse("2026-11-01")).isInverted()).isFalse();
  }
}
