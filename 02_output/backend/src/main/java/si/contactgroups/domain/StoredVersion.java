package si.contactgroups.domain;

import java.time.Instant;

/** A row of {@code contact_version}: the stored state as JSON text, with who saved it and when. */
public record StoredVersion(int number, Instant createdAt, String userName, String snapshot) {}
