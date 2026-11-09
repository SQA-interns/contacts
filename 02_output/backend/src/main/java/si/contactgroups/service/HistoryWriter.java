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
  private static final String SYSTEM = "system";

  private final HistoryRepository history;
  private final UserRepository users;
  private final JsonMapper json;
  private final HistoryRedactor redactor;

  public HistoryWriter(
      HistoryRepository history, UserRepository users, JsonMapper json, HistoryRedactor redactor) {
    this.history = history;
    this.users = users;
    this.json = json;
    this.redactor = redactor;
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

  /** Records a Tag renamed or deleted on a Contact (R-006-07): old and new name. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void tagRenamed(
      UUID groupId, UUID userId, UUID contactId, String oldName, String newName, Instant at) {
    write(
        groupId,
        userId,
        HistoryAction.TAG_RENAMED,
        contactId,
        Map.of("old", oldName, "new", newName, "name", newName),
        at);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void tagDeleted(UUID groupId, UUID userId, UUID contactId, String name, Instant at) {
    write(groupId, userId, HistoryAction.TAG_DELETED, contactId, Map.of("name", name), at);
  }

  /** Records the move of a Contact to Trash (R-012-07). */
  @Transactional(propagation = Propagation.MANDATORY)
  public void contactDeleted(UUID groupId, UUID userId, UUID contactId, Instant at) {
    write(groupId, userId, HistoryAction.CONTACT_DELETED, contactId, Map.of(), at);
  }

  /** Records the return of a Contact from Trash (R-012-07). */
  @Transactional(propagation = Propagation.MANDATORY)
  public void contactRestored(UUID groupId, UUID userId, UUID contactId, Instant at) {
    write(groupId, userId, HistoryAction.CONTACT_RESTORED, contactId, Map.of(), at);
  }

  /**
   * Replaces the values in the earlier entries of the Contact by "removed", then records the
   * permanent deletion without any value of the Contact (R-013-06, R-013-08). {@code userId} is
   * null when the clean-up job deletes (the user reads "system").
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void contactDeletedForever(UUID groupId, UUID userId, UUID contactId, Instant at) {
    history
        .detailsOfContact(groupId, contactId)
        .forEach(e -> history.replaceDetails(groupId, e.id(), redactor.redact(e.details())));
    if (userId == null) {
      history.insert(
          groupId,
          HistoryAction.CONTACT_DELETED_FOREVER,
          contactId,
          "{}",
          new HistoryActor(null, SYSTEM, null, at));
    } else {
      write(groupId, userId, HistoryAction.CONTACT_DELETED_FOREVER, contactId, Map.of(), at);
    }
  }

  /** Records an export: the number of Contacts in the file (R-011-07). */
  @Transactional(propagation = Propagation.MANDATORY)
  public void contactsExported(UUID groupId, UUID userId, int count, Instant at) {
    write(groupId, userId, HistoryAction.CONTACTS_EXPORTED, null, Map.of("count", count), at);
  }

  /** Records an Invitation created: address and Role (R-014-11). */
  @Transactional(propagation = Propagation.MANDATORY)
  public void invitationCreated(UUID groupId, UUID userId, String email, String role, Instant at) {
    write(
        groupId,
        userId,
        HistoryAction.INVITATION_CREATED,
        null,
        Map.of("email", email, "role", role),
        at);
  }

  /** Records an Invitation cancelled (R-014-11). */
  @Transactional(propagation = Propagation.MANDATORY)
  public void invitationCancelled(UUID groupId, UUID userId, String email, Instant at) {
    write(groupId, userId, HistoryAction.INVITATION_CANCELLED, null, Map.of("email", email), at);
  }

  /** Records a User joining through an Invitation, with the Role (R-014-11). */
  @Transactional(propagation = Propagation.MANDATORY)
  public void memberJoined(UUID groupId, UUID userId, String role, Instant at) {
    write(groupId, userId, HistoryAction.MEMBER_JOINED, null, Map.of("role", role), at);
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
