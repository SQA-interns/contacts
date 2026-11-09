package si.contactgroups.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import si.contactgroups.domain.StoredVersion;

/** The stored Versions of Contacts (table {@code contact_version}, ADR-07). */
@Repository
public class VersionRepository {

  private static final String CONTACT_ID = "contactId";
  private static final String GROUP_ID = "groupId";

  /**
   * The Versions of a Contact of the Group with the name of the User who saved them; when the User
   * is gone, the name kept in the History (R-021-03).
   */
  private static final String SELECT_VERSIONS =
      "SELECT v.number, v.created_at, CAST(v.snapshot AS text) AS snapshot,"
          + " coalesce(u.display_name, (SELECT h.user_name FROM history_entry h"
          + " WHERE h.contact_id = v.contact_id AND h.user_id = v.user_id"
          + " ORDER BY h.occurred_at DESC LIMIT 1), '') AS user_name"
          + " FROM contact_version v JOIN contact c ON c.id = v.contact_id"
          + " LEFT JOIN app_user u ON u.id = v.user_id"
          + " WHERE c.group_id = :groupId AND v.contact_id = :contactId";

  private final JdbcClient jdbc;

  public VersionRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Stores the Contact as it is now as Version {@code number}: its fields, Tags and Attributes as
   * JSON.
   */
  public void insertSnapshot(UUID groupId, UUID contactId, int number, UUID userId, Instant at) {
    jdbc.sql(
            "INSERT INTO contact_version (contact_id, number, created_at, user_id, snapshot)"
                + " SELECT c.id, :number, :at, :userId, jsonb_build_object("
                + "'firstName', c.first_name, 'lastName', c.last_name, 'email', c.email,"
                + " 'phone', c.phone, 'company', c.company, 'jobTitle', c.job_title,"
                + " 'street', c.street, 'postalCode', c.postal_code, 'city', c.city,"
                + " 'country', c.country, 'notes', c.notes,"
                + " 'tags', (SELECT coalesce(jsonb_agg(t.name ORDER BY lower(t.name)), '[]'::jsonb)"
                + " FROM contact_tag ct JOIN tag t ON t.id = ct.tag_id WHERE ct.contact_id = c.id),"
                + " 'attributes', (SELECT coalesce(jsonb_agg(jsonb_build_object("
                + "'key', a.key, 'value', a.value) ORDER BY lower(a.key)), '[]'::jsonb)"
                + " FROM attribute a WHERE a.contact_id = c.id))"
                + " FROM contact c WHERE c.id = :contactId AND c.group_id = :groupId")
        .param(CONTACT_ID, contactId)
        .param(GROUP_ID, groupId)
        .param("number", number)
        .param("userId", userId)
        .param("at", Timestamp.from(at))
        .update();
  }

  /** The Versions of a Contact, newest first (R-022-02). */
  public List<StoredVersion> list(UUID groupId, UUID contactId) {
    return jdbc.sql(SELECT_VERSIONS + " ORDER BY v.number DESC")
        .param(GROUP_ID, groupId)
        .param(CONTACT_ID, contactId)
        .query(VersionRepository::row)
        .list();
  }

  /** One Version of a Contact; empty when there is none with this number. */
  public Optional<StoredVersion> find(UUID groupId, UUID contactId, int number) {
    return jdbc.sql(SELECT_VERSIONS + " AND v.number = :number")
        .param(GROUP_ID, groupId)
        .param(CONTACT_ID, contactId)
        .param("number", number)
        .query(VersionRepository::row)
        .optional();
  }

  private static StoredVersion row(ResultSet rs, int n) throws SQLException {
    return new StoredVersion(
        rs.getInt("number"),
        rs.getTimestamp("created_at").toInstant(),
        rs.getString("user_name"),
        rs.getString("snapshot"));
  }
}
