package si.contactgroups.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import si.contactgroups.domain.AttributeChanges;
import si.contactgroups.domain.ContactChanges;
import si.contactgroups.domain.ContactData;
import si.contactgroups.domain.HistoryAction;
import si.contactgroups.domain.HistoryActor;
import si.contactgroups.persistence.HistoryRepository;
import si.contactgroups.persistence.UserRepository;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * The only way entries enter the History (ADR-08). It joins the transaction of the audited change,
 * so a failed action leaves no entry (BR-03).
 */
@Service
public class HistoryWriter {

  private static final int NAME_MAX = 100;

  private final HistoryRepository history;
  private final UserRepository users;
  private final JsonMapper json;

  public HistoryWriter(HistoryRepository history, UserRepository users, JsonMapper json) {
    this.history = history;
    this.users = users;
    this.json = json;
  }

  /** Records the creation with the stored values (R-016-01). */
  @Transactional(propagation = Propagation.MANDATORY)
  public void contactCreated(
      UUID groupId, UUID userId, UUID contactId, ContactData data, Instant at) {
    write(
        groupId,
        userId,
        HistoryAction.CONTACT_CREATED,
        contactId,
        Map.of("values", ContactChanges.stored(data)),
        at);
  }

  /** Records an edit with the old and new value of every changed field (R-016-02). */
  @Transactional(propagation = Propagation.MANDATORY)
  public void contactEdited(
      UUID groupId,
      UUID userId,
      UUID contactId,
      ContactData before,
      ContactData after,
      Instant at) {
    Map<String, ContactChanges.Change> changes = ContactChanges.between(before, after);
    if (changes.isEmpty()) {
      return;
    }
    ObjectNode pairs = json.createObjectNode();
    changes.forEach(
        (field, change) -> {
          ObjectNode pair = pairs.putObject(field);
          pair.put("old", change.oldValue());
          pair.put("new", change.newValue());
        });
    write(groupId, userId, HistoryAction.CONTACT_EDITED, contactId, Map.of("changes", pairs), at);
  }

  /** Records an Attribute added, changed or removed: key, old and new value (R-004-06). */
  @Transactional(propagation = Propagation.MANDATORY)
  public void attributeChanged(
      UUID groupId, UUID userId, UUID contactId, AttributeChanges.Change change, Instant at) {
    Map<String, String> details = new LinkedHashMap<>();
    details.put("key", change.key());
    if (change.oldValue() != null) {
      details.put("old", change.oldValue());
    }
    if (change.newValue() != null) {
      details.put("new", change.newValue());
    }
    write(groupId, userId, change.action(), contactId, details, at);
  }

  /** Records a Tag put on or taken off a Contact: its name (R-005-08). */
  @Transactional(propagation = Propagation.MANDATORY)
  public void tagChanged(
      UUID groupId, UUID userId, UUID contactId, HistoryAction action, String name, Instant at) {
    write(groupId, userId, action, contactId, Map.of("name", name), at);
  }

  private void write(
      UUID groupId,
      UUID userId,
      HistoryAction action,
      UUID contactId,
      Map<String, ?> details,
      Instant at) {
    var user = users.findById(userId).orElseThrow();
    String name =
        user.displayName() == null || user.displayName().isBlank()
            ? user.email()
            : user.displayName();
    if (name.length() > NAME_MAX) {
      name = name.substring(0, NAME_MAX);
    }
    history.insert(
        groupId,
        action,
        contactId,
        json.writeValueAsString(details),
        new HistoryActor(userId, name, user.email(), at));
  }
}
