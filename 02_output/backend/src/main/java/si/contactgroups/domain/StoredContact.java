package si.contactgroups.domain;

import java.time.Instant;
import java.util.UUID;

/** A stored Contact row: the fields and the version number, without Tags and Attributes. */
public record StoredContact(
    UUID id, int version, ContactData data, Instant createdAt, Instant updatedAt) {}
