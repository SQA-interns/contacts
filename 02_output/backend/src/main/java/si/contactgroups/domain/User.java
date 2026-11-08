package si.contactgroups.domain;

import java.time.Instant;
import java.util.UUID;

/** A person who has signed in at least once (glossary: User; R-001-11). */
public record User(
    UUID id, String firebaseUid, String email, String displayName, Instant createdAt) {}
