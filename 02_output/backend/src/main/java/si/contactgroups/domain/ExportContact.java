package si.contactgroups.domain;

import java.util.List;

/** A Contact as the Export file needs it: the 11 fields, the Tag names and the Attributes. */
public record ExportContact(ContactData data, List<String> tags, List<AttributePair> attributes) {}
