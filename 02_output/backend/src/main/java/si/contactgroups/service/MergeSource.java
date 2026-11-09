package si.contactgroups.service;

import java.util.List;
import java.util.UUID;
import si.contactgroups.domain.AttributePair;
import si.contactgroups.domain.ContactData;

/** One Contact of a merge as the service read it: its fields, Attributes and Tags (US-026). */
record MergeSource(UUID id, ContactData data, List<AttributePair> attributes, List<String> tags) {}
