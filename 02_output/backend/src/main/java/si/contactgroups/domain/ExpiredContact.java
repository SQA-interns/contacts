package si.contactgroups.domain;

import java.util.UUID;

/** A Contact in Trash past the 30 days (BR-04): its Group and its id. */
public record ExpiredContact(UUID groupId, UUID contactId) {}
