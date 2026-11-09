package si.contactgroups.domain;

import java.util.List;

/** The content of a Contact at one point: its fields, Tags and Attributes (glossary: Version). */
public record VersionContent(ContactData data, List<String> tags, List<AttributePair> attributes) {}
