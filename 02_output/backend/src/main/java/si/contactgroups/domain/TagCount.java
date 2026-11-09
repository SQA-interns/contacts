package si.contactgroups.domain;

import java.util.UUID;

/** A Tag of a Group with the number of Contacts that carry it (R-006-01). */
public record TagCount(UUID id, String name, int contactCount) {}
