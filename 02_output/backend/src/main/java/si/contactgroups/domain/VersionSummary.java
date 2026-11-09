package si.contactgroups.domain;

import java.time.Instant;

/** One line of the Versions tab (contract {@code VersionSummary}, R-022-02). */
public record VersionSummary(int number, Instant createdAt, String userName) {}
