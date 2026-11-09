package si.contactgroups.domain;

import java.time.Instant;
import java.util.UUID;

/** An Invitation of an e-mail address to a Group with a Role (glossary: Invitation). */
public record Invitation(
    UUID id,
    String email,
    Role role,
    InvitationStatus status,
    Instant createdAt,
    Instant expiresAt) {}
