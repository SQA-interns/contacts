package si.contactgroups.domain;

import java.util.UUID;

/** A pending, unexpired Invitation that a verified User may join through (R-014-05). */
public record AcceptableInvitation(UUID id, UUID groupId, Role role) {}
