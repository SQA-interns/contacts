package si.contactgroups.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import si.contactgroups.domain.GroupSummary;

/** Groups (glossary: Group, table {@code contact_group}). */
@Repository
public class GroupRepository {

  private final JdbcClient jdbc;

  public GroupRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /** The Groups not in Trash of which the User is a Member, with the User's Role. */
  @GlobalScope
  public List<GroupSummary> findGroupsOfUser(UUID userId) {
    return jdbc.sql(
            "SELECT g.id, g.name, m.role, g.created_at FROM contact_group g"
                + " JOIN member m ON m.group_id = g.id"
                + " WHERE m.user_id = :userId AND g.deleted_at IS NULL"
                + " ORDER BY lower(g.name), g.id")
        .param("userId", userId)
        .query(GroupSummary.class)
        .list();
  }

  /** The name of the Group. */
  public String name(UUID groupId) {
    return jdbc.sql("SELECT name FROM contact_group WHERE id = :groupId")
        .param("groupId", groupId)
        .query(String.class)
        .single();
  }

  /** Stores a new Group and returns its id; the Group has no Member yet. */
  @GlobalScope
  public UUID insert(String name, Instant createdAt) {
    return jdbc.sql(
            "INSERT INTO contact_group (name, created_at) VALUES (:name, :createdAt) RETURNING id")
        .param("name", name)
        .param("createdAt", Timestamp.from(createdAt))
        .query(UUID.class)
        .single();
  }

  /** Whether the User owns a Group not in Trash with this name, ignoring case (BR-05). */
  @GlobalScope
  public boolean ownerHasGroupNamed(UUID userId, String name) {
    return jdbc.sql(
            "SELECT EXISTS (SELECT 1 FROM contact_group g JOIN member m ON m.group_id = g.id"
                + " WHERE m.user_id = :userId AND m.role = 'OWNER' AND g.deleted_at IS NULL"
                + " AND lower(g.name) = lower(:name))")
        .param("userId", userId)
        .param("name", name)
        .query(Boolean.class)
        .single();
  }
}
