package si.contactgroups.domain;

import java.time.Instant;
import java.util.UUID;

/** Who did it and when; name and e-mail are copied as they are at that time (R-016-06). */
public record HistoryActor(UUID userId, String name, String email, Instant at) {}
