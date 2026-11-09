package si.contactgroups.domain;

import java.time.Instant;
import java.util.UUID;

/** A User in a Group with a Role (glossary: Member). */
public record Member(UUID userId, String displayName, String email, Role role, Instant joinedAt) {}
