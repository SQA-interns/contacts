package si.contactgroups.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import si.contactgroups.domain.ActivityFilter;
import si.contactgroups.domain.HistoryAction;
import si.contactgroups.domain.HistoryActor;
import si.contactgroups.domain.HistoryDetails;
import si.contactgroups.domain.HistoryEntry;

/** The History entries of a Group (table {@code history_entry}); append-only (BR-03, ADR-03). */
@Repository
public class HistoryRepository {

  private static final String GROUP_ID = "groupId";
  private static final String CONTACT_ID = "contactId";
  private static final String DETAILS = "details";
  private static final String ACTION = "action";

  private static final String ACTIVITY_WHERE =
      "group_id = :groupId AND (CAST(:userId AS uuid) IS NULL OR user_id = CAST(:userId AS uuid))"
          + " AND (CAST(:action AS text) IS NULL OR action = CAST(:action AS text))"
          + " AND (CAST(:from AS timestamptz) IS NULL OR occurred_at >= CAST(:from AS timestamptz))"
          + " AND (CAST(:to AS timestamptz) IS NULL OR occurred_at < CAST(:to AS timestamptz))";

  private final JdbcClient jdbc;

  public HistoryRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /** Appends one entry; {@code detailsJson} is the JSON text of {@code details}. */
  public void insert(
      UUID groupId, HistoryAction action, UUID contactId, String detailsJson, HistoryActor actor) {
    jdbc.sql(
            "INSERT INTO history_entry (group_id, occurred_at, user_id, user_name, user_email,"
                + " action, contact_id, details) VALUES (:groupId, :at, :userId, :name, :email,"
                + " :action, :contactId, CAST(:details AS jsonb))")
        .param(GROUP_ID, groupId)
        .param("at", Timestamp.from(actor.at()))
        .param("userId", actor.userId())
        .param("name", actor.name())
        .param("email", actor.email())
        .param(ACTION, action.name())
        .param(CONTACT_ID, contactId)
        .param(DETAILS, detailsJson)
        .update();
  }

  /** One page of the entries of a Contact, newest first. */
  public List<HistoryEntry> pageOfContact(UUID groupId, UUID contactId, int limit, int offset) {
    return jdbc.sql(
            "SELECT id, occurred_at, user_name, user_email, action, contact_id,"
                + " CAST(details AS text) AS details FROM history_entry"
                + " WHERE group_id = :groupId AND contact_id = :contactId"
                + " ORDER BY occurred_at DESC, id DESC LIMIT :limit OFFSET :offset")
        .param(GROUP_ID, groupId)
        .param(CONTACT_ID, contactId)
        .param("limit", limit)
        .param("offset", offset)
        .query(HistoryRepository::entry)
        .list();
  }

  public long countOfContact(UUID groupId, UUID contactId) {
    return jdbc.sql(
            "SELECT count(*) FROM history_entry WHERE group_id = :groupId"
                + " AND contact_id = :contactId")
        .param(GROUP_ID, groupId)
        .param(CONTACT_ID, contactId)
        .query(Long.class)
        .single();
  }

  /** One page of the entries of a Group, newest first; a {@code null} filter is not applied. */
  public List<HistoryEntry> pageOfGroup(
      UUID groupId, ActivityFilter filter, int limit, int offset) {
    return jdbc.sql(
            "SELECT id, occurred_at, user_name, user_email, action, contact_id,"
                + " CAST(details AS text) AS details FROM history_entry WHERE "
                + ACTIVITY_WHERE
                + " ORDER BY occurred_at DESC, id DESC LIMIT :limit OFFSET :offset")
        .param(GROUP_ID, groupId)
        .param("userId", filter.userId())
        .param(ACTION, filter.action() == null ? null : filter.action().name())
        .param("from", toTimestamp(filter.fromInclusive()))
        .param("to", toTimestamp(filter.toExclusive()))
        .param("limit", limit)
        .param("offset", offset)
        .query(HistoryRepository::entry)
        .list();
  }

  public long countOfGroup(UUID groupId, ActivityFilter filter) {
    return jdbc.sql("SELECT count(*) FROM history_entry WHERE " + ACTIVITY_WHERE)
        .param(GROUP_ID, groupId)
        .param("userId", filter.userId())
        .param(ACTION, filter.action() == null ? null : filter.action().name())
        .param("from", toTimestamp(filter.fromInclusive()))
        .param("to", toTimestamp(filter.toExclusive()))
        .query(Long.class)
        .single();
  }

  private static Timestamp toTimestamp(Instant at) {
    return at == null ? null : Timestamp.from(at);
  }

  private static HistoryEntry entry(java.sql.ResultSet rs, int unused)
      throws java.sql.SQLException {
    return new HistoryEntry(
        rs.getObject("id", UUID.class),
        rs.getTimestamp("occurred_at").toInstant(),
        rs.getString("user_name"),
        rs.getString("user_email"),
        HistoryAction.valueOf(rs.getString(ACTION)),
        rs.getObject("contact_id", UUID.class),
        rs.getString(DETAILS));
  }

  /** The id and details (JSON text) of every entry of a Contact. */
  public List<HistoryDetails> detailsOfContact(UUID groupId, UUID contactId) {
    return jdbc.sql(
            "SELECT id, CAST(details AS text) AS details FROM history_entry"
                + " WHERE group_id = :groupId AND contact_id = :contactId")
        .param(GROUP_ID, groupId)
        .param(CONTACT_ID, contactId)
        .query((rs, n) -> new HistoryDetails(rs.getObject("id", UUID.class), rs.getString(DETAILS)))
        .list();
  }

  /** Replaces the details of one entry; the only change the append-only History allows. */
  public void replaceDetails(UUID groupId, UUID entryId, String detailsJson) {
    jdbc.sql(
            "UPDATE history_entry SET details = CAST(:details AS jsonb)"
                + " WHERE id = :id AND group_id = :groupId")
        .param(DETAILS, detailsJson)
        .param("id", entryId)
        .param(GROUP_ID, groupId)
        .update();
  }
}
