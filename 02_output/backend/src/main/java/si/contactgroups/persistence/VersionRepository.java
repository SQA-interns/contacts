package si.contactgroups.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** The stored Versions of Contacts (table {@code contact_version}, ADR-07). */
@Repository
public class VersionRepository {

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
        .param("contactId", contactId)
        .param("groupId", groupId)
        .param("number", number)
        .param("userId", userId)
        .param("at", Timestamp.from(at))
        .update();
  }
}
