package si.contactgroups.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import si.contactgroups.domain.AttributePair;
import si.contactgroups.domain.Contact;
import si.contactgroups.domain.ContactData;
import si.contactgroups.domain.StoredVersion;
import si.contactgroups.domain.VersionComparison;
import si.contactgroups.domain.VersionContent;
import si.contactgroups.domain.VersionDetail;
import si.contactgroups.domain.VersionDiff;
import si.contactgroups.domain.VersionSummary;
import si.contactgroups.persistence.VersionRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the Versions of a Contact (US-022). A Version is written by every save ({@code
 * ContactService}) and is never changed here (R-022-05); the Contact in Trash has none to read
 * (EX-022-04) and a Contact deleted forever takes its Versions with it (R-022-07, cascade).
 */
@Service
public class VersionService {

  private final VersionRepository versions;
  private final ContactService contacts;
  private final JsonMapper json;

  public VersionService(VersionRepository versions, ContactService contacts, JsonMapper json) {
    this.versions = versions;
    this.contacts = contacts;
    this.json = json;
  }

  /** The Versions of the Contact, newest first (R-022-02). */
  public List<VersionSummary> list(UUID groupId, UUID contactId) {
    contacts.get(groupId, contactId);
    return versions.list(groupId, contactId).stream()
        .map(v -> new VersionSummary(v.number(), v.createdAt(), v.userName()))
        .toList();
  }

  /** One earlier Version with its fields, Tags and Attributes (R-022-03). */
  public VersionDetail get(UUID groupId, UUID contactId, String number) {
    Contact live = contacts.get(groupId, contactId);
    StoredVersion stored = find(groupId, contactId, number);
    VersionContent content = parse(stored.snapshot());
    return new VersionDetail(
        stored.number(),
        stored.createdAt(),
        stored.userName(),
        new Contact(
            contactId,
            stored.number(),
            content.data(),
            content.tags(),
            content.attributes(),
            live.createdAt(),
            stored.createdAt()));
  }

  /** What differs between the Version and the live Contact, with both values (R-022-04). */
  public VersionComparison compare(UUID groupId, UUID contactId, String number) {
    Contact live = contacts.get(groupId, contactId);
    StoredVersion stored = find(groupId, contactId, number);
    VersionContent current = new VersionContent(live.data(), live.tags(), live.attributes());
    return new VersionComparison(
        stored.number(), VersionDiff.between(parse(stored.snapshot()), current));
  }

  private StoredVersion find(UUID groupId, UUID contactId, String number) {
    try {
      return versions
          .find(groupId, contactId, Integer.parseInt(number))
          .orElseThrow(VersionNotFoundException::new);
    } catch (NumberFormatException e) {
      throw new VersionNotFoundException();
    }
  }

  private VersionContent parse(String snapshot) {
    JsonNode node = json.readTree(snapshot);
    List<String> tags = new ArrayList<>();
    node.path("tags").forEach(tag -> tags.add(tag.asString()));
    List<AttributePair> attributes = new ArrayList<>();
    node.path("attributes")
        .forEach(pair -> attributes.add(new AttributePair(text(pair, "key"), text(pair, "value"))));
    return new VersionContent(data(node), tags, attributes);
  }

  private static ContactData data(JsonNode node) {
    return new ContactData(
        text(node, "firstName"),
        text(node, "lastName"),
        text(node, "email"),
        text(node, "phone"),
        text(node, "company"),
        text(node, "jobTitle"),
        text(node, "street"),
        text(node, "postalCode"),
        text(node, "city"),
        text(node, "country"),
        text(node, "notes"));
  }

  private static String text(JsonNode node, String name) {
    JsonNode value = node.path(name);
    return value.isNull() || value.isMissingNode() ? null : value.asString();
  }
}
