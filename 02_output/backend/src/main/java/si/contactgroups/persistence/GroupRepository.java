package si.contactgroups.persistence;

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
}
