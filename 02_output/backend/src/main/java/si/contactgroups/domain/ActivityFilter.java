package si.contactgroups.domain;

import java.time.Instant;
import java.util.UUID;

/** The filters of the Group activity list; a {@code null} value does not filter (R-017-05). */
public record ActivityFilter(
    UUID userId, HistoryAction action, Instant fromInclusive, Instant toExclusive) {}
