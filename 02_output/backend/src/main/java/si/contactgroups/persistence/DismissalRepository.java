package si.contactgroups.persistence;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** The Duplicate sets marked "not a duplicate" (US-025); every method is scoped to one Group. */
@Repository
public class DismissalRepository {

  private static final String GROUP_ID = "groupId";

  private final JdbcClient jdbc;

  public DismissalRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /** The Contacts of every dismissed set of the Group, one entry per dismissal. */
  public List<Set<UUID>> dismissedSets(UUID groupId) {
    Map<UUID, Set<UUID>> byDismissal = new HashMap<>();
    jdbc.sql(
            "SELECT dc.dismissal_id, dc.contact_id FROM duplicate_dismissal d"
                + " JOIN duplicate_dismissal_contact dc ON dc.dismissal_id = d.id"
                + " WHERE d.group_id = :groupId")
        .param(GROUP_ID, groupId)
        .query(
            rs -> {
              byDismissal
                  .computeIfAbsent(rs.getObject(1, UUID.class), k -> new HashSet<>())
                  .add(rs.getObject(2, UUID.class));
            });
    return List.copyOf(byDismissal.values());
  }

  /**
   * Stores the set once: a second dismissal of the same key does nothing, also when two run at the
   * same time (EX-025-04).
   */
  public void add(UUID groupId, String key, UUID userId, Collection<UUID> contactIds) {
    UUID id =
        jdbc.sql(
                "INSERT INTO duplicate_dismissal (group_id, contact_key, created_by)"
                    + " VALUES (:groupId, :key, :userId)"
                    + " ON CONFLICT (group_id, contact_key) DO NOTHING RETURNING id")
            .param(GROUP_ID, groupId)
            .param("key", key)
            .param("userId", userId)
            .query(UUID.class)
            .optional()
            .orElse(null);
    if (id == null) {
      return;
    }
    for (UUID contactId : contactIds) {
      jdbc.sql(
              "INSERT INTO duplicate_dismissal_contact (dismissal_id, contact_id)"
                  + " VALUES (:id, :contactId)")
          .param("id", id)
          .param("contactId", contactId)
          .update();
    }
  }
}
