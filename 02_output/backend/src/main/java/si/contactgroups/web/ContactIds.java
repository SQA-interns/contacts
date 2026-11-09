package si.contactgroups.web;

import java.util.UUID;
import si.contactgroups.service.ContactNotFoundException;

/** Reads a Contact id from a path; a text that is no id is "not found" (R-018-05). */
final class ContactIds {

  private ContactIds() {}

  static UUID parse(String contactId) {
    try {
      return UUID.fromString(contactId);
    } catch (IllegalArgumentException e) {
      throw new ContactNotFoundException();
    }
  }
}
