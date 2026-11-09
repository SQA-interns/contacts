package si.contactgroups.service;

import java.util.List;
import java.util.UUID;
import si.contactgroups.domain.AttributePair;
import si.contactgroups.domain.ContactData;

/**
 * A merge as the user confirmed it (contract schema {@code MergeRequest}): the Survivor, the
 * Contacts with the version number each had on the Merge screen, the chosen fields and the chosen
 * Attribute values.
 */
public record MergeCommand(
    UUID survivorId, List<Target> contacts, ContactData fields, List<AttributePair> attributes) {

  /** One merged Contact with the version number the user saw (BR-07). */
  public record Target(UUID id, Integer version) {}
}
