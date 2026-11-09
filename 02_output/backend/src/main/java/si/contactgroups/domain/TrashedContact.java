package si.contactgroups.domain;

import java.time.Instant;
import java.util.UUID;

/** A Contact in Trash as the Trash view lists it (R-012-03); {@code deletedBy} may be unknown. */
public record TrashedContact(
    UUID id,
    String firstName,
    String lastName,
    String email,
    String company,
    Instant deletedAt,
    String deletedBy) {}
