package si.contactgroups.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import si.contactgroups.domain.AttributePair;
import si.contactgroups.domain.Normaliser;

/** The Attributes of Contacts (table {@code attribute}); every method is scoped to one Group. */
@Repository
public class AttributeRepository {

  private static final String GROUP_ID = "groupId";
  private static final String CONTACT_ID = "contactId";
  private static final String IN_GROUP =
      " contact_id IN (SELECT id FROM contact WHERE id = :contactId AND group_id = :groupId)";

  private final JdbcClient jdbc;

  public AttributeRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /** The pairs of the Contact ordered by key ignoring case. */
  public List<AttributePair> list(UUID groupId, UUID contactId) {
    return jdbc.sql(
            "SELECT key, value FROM attribute WHERE" + IN_GROUP + " ORDER BY lower(key), key")
        .param(GROUP_ID, groupId)
        .param(CONTACT_ID, contactId)
        .query((rs, n) -> new AttributePair(rs.getString("key"), rs.getString("value")))
        .list();
  }

  public void insert(UUID groupId, UUID contactId, AttributePair pair) {
    jdbc.sql(
            "INSERT INTO attribute (contact_id, key, value, value_norm)"
                + " SELECT id, :key, :value, :valueNorm FROM contact"
                + " WHERE id = :contactId AND group_id = :groupId")
        .param(GROUP_ID, groupId)
        .param(CONTACT_ID, contactId)
        .param("key", pair.key())
        .param("value", pair.value())
        .param("valueNorm", Normaliser.normalise(pair.value()))
        .update();
  }

  /** Sets the value of the pair whose key equals {@code key} ignoring case. */
  public void updateValue(UUID groupId, UUID contactId, String key, String value) {
    jdbc.sql(
            "UPDATE attribute SET value = :value, value_norm = :valueNorm"
                + " WHERE lower(key) = lower(:key) AND"
                + IN_GROUP)
        .param(GROUP_ID, groupId)
        .param(CONTACT_ID, contactId)
        .param("key", key)
        .param("value", value)
        .param("valueNorm", Normaliser.normalise(value))
        .update();
  }

  /** Removes the pair whose key equals {@code key} ignoring case. */
  public void delete(UUID groupId, UUID contactId, String key) {
    jdbc.sql("DELETE FROM attribute WHERE lower(key) = lower(:key) AND" + IN_GROUP)
        .param(GROUP_ID, groupId)
        .param(CONTACT_ID, contactId)
        .param("key", key)
        .update();
  }
}
