package si.contactgroups.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import si.contactgroups.domain.AcceptableInvitation;
import si.contactgroups.domain.Invitation;
import si.contactgroups.domain.Role;

/** The Invitations of a Group (glossary: Invitation, table {@code invitation}). */
@Repository
public class InvitationRepository {

  private static final String GROUP_ID = "groupId";
  private static final String NOW = "now";
  private static final String COLUMNS = "id, email, role, status, created_at, expires_at";
  private static final String PENDING =
      " group_id = :groupId AND status = 'PENDING' AND expires_at > :now";

  private final JdbcClient jdbc;

  public InvitationRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /** Stores a pending Invitation; {@code email} is already lowercase. */
  public Invitation insert(
      UUID groupId, String email, Role role, UUID createdBy, Instant createdAt, Instant expiresAt) {
    return jdbc.sql(
            "INSERT INTO invitation (group_id, email, role, status, created_at, expires_at,"
                + " created_by) VALUES (:groupId, :email, :role, 'PENDING', :createdAt,"
                + " :expiresAt, :createdBy) RETURNING "
                + COLUMNS)
        .param(GROUP_ID, groupId)
        .param("email", email)
        .param("role", role.name())
        .param("createdAt", Timestamp.from(createdAt))
        .param("expiresAt", Timestamp.from(expiresAt))
        .param("createdBy", createdBy)
        .query(Invitation.class)
        .single();
  }

  /** The pending Invitations that have not expired at {@code now}, oldest first (R-014-09). */
  public List<Invitation> findPending(UUID groupId, Instant now) {
    return jdbc.sql(
            "SELECT "
                + COLUMNS
                + " FROM invitation WHERE"
                + PENDING
                + " ORDER BY created_at, id")
        .param(GROUP_ID, groupId)
        .param(NOW, Timestamp.from(now))
        .query(Invitation.class)
        .list();
  }

  public int countPending(UUID groupId, Instant now) {
    return jdbc.sql("SELECT count(*) FROM invitation WHERE" + PENDING)
        .param(GROUP_ID, groupId)
        .param(NOW, Timestamp.from(now))
        .query(Integer.class)
        .single();
  }

  /** Whether a pending, unexpired Invitation of this lowercase address exists (R-014-04). */
  public boolean existsPending(UUID groupId, String email, Instant now) {
    return jdbc.sql(
            "SELECT EXISTS (SELECT 1 FROM invitation WHERE" + PENDING + " AND lower(email) = :email)")
        .param(GROUP_ID, groupId)
        .param(NOW, Timestamp.from(now))
        .param("email", email)
        .query(Boolean.class)
        .single();
  }

  /** Marks a pending, unexpired Invitation cancelled; empty when there is none (R-014-07). */
  public Optional<Invitation> cancel(UUID groupId, UUID invitationId, Instant now) {
    return jdbc.sql(
            "UPDATE invitation SET status = 'CANCELLED' WHERE id = :id AND"
                + PENDING
                + " RETURNING "
                + COLUMNS)
        .param("id", invitationId)
        .param(GROUP_ID, groupId)
        .param(NOW, Timestamp.from(now))
        .query(Invitation.class)
        .optional();
  }

  /**
   * The pending, unexpired Invitations of a Group not in Trash for this address, ignoring case,
   * locked until the end of the transaction (R-014-05, BR-05).
   */
  @GlobalScope
  public List<AcceptableInvitation> lockAcceptableFor(String email, Instant now) {
    return jdbc.sql(
            "SELECT i.id, i.group_id, i.role FROM invitation i"
                + " JOIN contact_group g ON g.id = i.group_id"
                + " WHERE lower(i.email) = lower(:email) AND i.status = 'PENDING'"
                + " AND i.expires_at > :now AND g.deleted_at IS NULL"
                + " ORDER BY i.created_at, i.id FOR UPDATE OF i")
        .param("email", email)
        .param(NOW, Timestamp.from(now))
        .query(
            (rs, n) ->
                new AcceptableInvitation(
                    rs.getObject("id", UUID.class),
                    rs.getObject("group_id", UUID.class),
                    Role.valueOf(rs.getString("role"))))
        .list();
  }

  @GlobalScope
  public void markAccepted(UUID invitationId) {
    jdbc.sql("UPDATE invitation SET status = 'ACCEPTED' WHERE id = :id")
        .param("id", invitationId)
        .update();
  }
}
