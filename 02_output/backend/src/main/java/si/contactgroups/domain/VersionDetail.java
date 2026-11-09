package si.contactgroups.domain;

import java.time.Instant;

/** An earlier Version with its content (contract {@code VersionDetail}, R-022-03). */
public record VersionDetail(int number, Instant createdAt, String userName, Contact content) {}
