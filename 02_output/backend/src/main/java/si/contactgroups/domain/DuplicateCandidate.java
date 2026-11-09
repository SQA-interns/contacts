package si.contactgroups.domain;

/** A Contact of the Group not in Trash with the phone number the list item does not carry. */
public record DuplicateCandidate(ContactListItem item, String phone) {}
