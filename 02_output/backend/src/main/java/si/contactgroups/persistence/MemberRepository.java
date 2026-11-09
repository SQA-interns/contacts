package si.contactgroups.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import si.contactgroups.domain.Member;
import si.contactgroups.domain.Role;

/** The Members of a Group (glossary: Member, table {@code member}). */
@Repository
public class MemberRepository {

  private static final String GROUP_ID = "groupId";
  private static final String USER_ID = "userId";

  private final JdbcClient jdbc;

  public MemberRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public void insert(UUID groupId, UUID userId, Role role, Instant joinedAt) {
    jdbc.sql(
            "INSERT INTO member (group_id, user_id, role, joined_at)"
                + " VALUES (:groupId, :userId, :role, :joinedAt)")
        .param(GROUP_ID, groupId)
        .param(USER_ID, userId)
        .param("role", role.name())
        .param("joinedAt", Timestamp.from(joinedAt))
        .update();
  }

  /** The Role of the User in a Group that is not in Trash; empty when there is none (R-018-02). */
  public Optional<Role> findRole(UUID groupId, UUID userId) {
    return jdbc.sql(
            "SELECT m.role FROM member m JOIN contact_group g ON g.id = m.group_id"
                + " WHERE m.group_id = :groupId AND m.user_id = :userId AND g.deleted_at IS NULL")
        .param(GROUP_ID, groupId)
        .param(USER_ID, userId)
        .query(String.class)
        .optional()
        .map(Role::valueOf);
  }

  /** Whether a Member of the Group has this e-mail address, ignoring case (R-014-04, BR-05). */
  public boolean existsByEmail(UUID groupId, String email) {
    return jdbc.sql(
            "SELECT EXISTS (SELECT 1 FROM member m JOIN app_user u ON u.id = m.user_id"
                + " WHERE m.group_id = :groupId AND lower(u.email) = lower(:email))")
        .param(GROUP_ID, groupId)
        .param("email", email)
        .query(Boolean.class)
        .single();
  }

  /** Whether the User is a Member of the Group. */
  public boolean exists(UUID groupId, UUID userId) {
    return jdbc.sql(
            "SELECT EXISTS (SELECT 1 FROM member WHERE group_id = :groupId AND user_id = :userId)")
        .param(GROUP_ID, groupId)
        .param(USER_ID, userId)
        .query(Boolean.class)
        .single();
  }

  /** Every Member of the Group, oldest first (R-015-01). */
  public List<Member> findAll(UUID groupId) {
    return jdbc.sql(
            "SELECT m.user_id, u.display_name, u.email, m.role, m.joined_at FROM member m"
                + " JOIN app_user u ON u.id = m.user_id WHERE m.group_id = :groupId"
                + " ORDER BY m.joined_at, u.display_name")
        .param(GROUP_ID, groupId)
        .query(
            (rs, n) ->
                new Member(
                    rs.getObject("user_id", UUID.class),
                    rs.getString("display_name"),
                    rs.getString("email"),
                    Role.valueOf(rs.getString("role")),
                    rs.getTimestamp("joined_at").toInstant()))
        .list();
  }

  /** The Member with this User id in the Group, if any. */
  public Optional<Member> find(UUID groupId, UUID userId) {
    return findAll(groupId).stream().filter(m -> m.userId().equals(userId)).findFirst();
  }

  public void updateRole(UUID groupId, UUID userId, Role role) {
    jdbc.sql("UPDATE member SET role = :role WHERE group_id = :groupId AND user_id = :userId")
        .param("role", role.name())
        .param(GROUP_ID, groupId)
        .param(USER_ID, userId)
        .update();
  }

  public void delete(UUID groupId, UUID userId) {
    jdbc.sql("DELETE FROM member WHERE group_id = :groupId AND user_id = :userId")
        .param(GROUP_ID, groupId)
        .param(USER_ID, userId)
        .update();
  }

  public int countOwners(UUID groupId) {
    return jdbc.sql("SELECT count(*) FROM member WHERE group_id = :groupId AND role = 'OWNER'")
        .param(GROUP_ID, groupId)
        .query(Integer.class)
        .single();
  }
}
