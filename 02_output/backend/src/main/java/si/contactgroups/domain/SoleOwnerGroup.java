package si.contactgroups.domain;

import java.util.UUID;

/**
 * Contract schema {@code SoleOwnerGroup}: a Group of which the User is the only Owner (R-021-02).
 * {@code daysLeft} is null unless the Group is in the Group Trash (D-27 A1).
 */
public record SoleOwnerGroup(UUID id, String name, boolean inTrash, Integer daysLeft) {}
