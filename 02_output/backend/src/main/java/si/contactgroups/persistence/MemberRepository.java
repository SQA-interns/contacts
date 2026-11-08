package si.contactgroups.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import si.contactgroups.domain.Role;

/** The Members of a Group (glossary: Member, table {@code member}). */
@Repository
public class MemberRepository {

  private final JdbcClient jdbc;

  public MemberRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public void insert(UUID groupId, UUID userId, Role role, Instant joinedAt) {
    jdbc.sql(
            "INSERT INTO member (group_id, user_id, role, joined_at)"
                + " VALUES (:groupId, :userId, :role, :joinedAt)")
        .param("groupId", groupId)
        .param("userId", userId)
        .param("role", role.name())
        .param("joinedAt", Timestamp.from(joinedAt))
        .update();
  }

  /** The Role of the User in a Group that is not in Trash; empty when there is none (R-018-02). */
  public Optional<Role> findRole(UUID groupId, UUID userId) {
    return jdbc.sql(
            "SELECT m.role FROM member m JOIN contact_group g ON g.id = m.group_id"
                + " WHERE m.group_id = :groupId AND m.user_id = :userId AND g.deleted_at IS NULL")
        .param("groupId", groupId)
        .param("userId", userId)
        .query(String.class)
        .optional()
        .map(Role::valueOf);
  }
}
