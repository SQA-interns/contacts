package si.contactgroups.domain;

import java.time.Instant;
import java.util.UUID;

/** A Group as its Member sees it in the Group list: with the Member's Role (R-002-05). */
public record GroupSummary(UUID id, String name, Role role, Instant createdAt) {}
