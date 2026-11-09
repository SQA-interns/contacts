package si.contactgroups.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import si.contactgroups.domain.GroupSummary;
import si.contactgroups.domain.GroupTrashItem;
import si.contactgroups.domain.SoleOwnerGroup;
import si.contactgroups.domain.TrashPeriod;

/** Groups (glossary: Group, table {@code contact_group}). */
@Repository
public class GroupRepository {

  private static final String GROUP_ID = "groupId";
  private static final String USER_ID = "userId";
  private static final String CUTOFF = "cutoff";

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
        .param(USER_ID, userId)
        .query(GroupSummary.class)
        .list();
  }

  /** Locks the Group row until the end of the transaction: Invitation checks then do not race. */
  public void lock(UUID groupId) {
    jdbc.sql("SELECT id FROM contact_group WHERE id = :groupId FOR UPDATE")
        .param(GROUP_ID, groupId)
        .query()
        .rowSet();
  }

  /** The name of the Group. */
  public String name(UUID groupId) {
    return jdbc.sql("SELECT name FROM contact_group WHERE id = :groupId")
        .param(GROUP_ID, groupId)
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
        .param(USER_ID, userId)
        .param("name", name)
        .query(Boolean.class)
        .single();
  }

  /** The name of the Group when it is not in Trash. */
  public Optional<String> activeName(UUID groupId) {
    return jdbc.sql("SELECT name FROM contact_group WHERE id = :groupId AND deleted_at IS NULL")
        .param(GROUP_ID, groupId)
        .query(String.class)
        .optional();
  }

  /** Moves the Group to the Group Trash; false when it is already there or does not exist. */
  public boolean moveToTrash(UUID groupId, Instant now) {
    return jdbc.sql(
                "UPDATE contact_group SET deleted_at = :now"
                    + " WHERE id = :groupId AND deleted_at IS NULL")
            .param("now", Timestamp.from(now))
            .param(GROUP_ID, groupId)
            .update()
        > 0;
  }

  /**
   * The Groups in Trash since after {@code cutoff} of which the User is an Owner, newest deletion
   * first (R-020-04); a Group at or past the cutoff is past the 30 days.
   */
  @GlobalScope
  public List<GroupTrashItem> findTrashOfOwner(UUID userId, Instant cutoff, Instant now) {
    return jdbc.sql(
            "SELECT g.id, g.name, g.deleted_at FROM contact_group g"
                + " JOIN member m ON m.group_id = g.id"
                + " WHERE m.user_id = :userId AND m.role = 'OWNER' AND g.deleted_at > :cutoff"
                + " ORDER BY g.deleted_at DESC, g.id")
        .param(USER_ID, userId)
        .param(CUTOFF, Timestamp.from(cutoff))
        .query(
            (rs, row) ->
                GroupTrashItem.of(
                    rs.getObject("id", UUID.class),
                    rs.getString("name"),
                    rs.getTimestamp("deleted_at").toInstant(),
                    now))
        .list();
  }

  /**
   * The Group in Trash since after {@code cutoff} of which the User is an Owner, shown as the User
   * will see it in the Group list after the restore (R-020-07).
   */
  @GlobalScope
  public Optional<GroupSummary> findRestorable(UUID groupId, UUID userId, Instant cutoff) {
    return jdbc.sql(
            "SELECT g.id, g.name, m.role, g.created_at FROM contact_group g"
                + " JOIN member m ON m.group_id = g.id"
                + " WHERE g.id = :groupId AND m.user_id = :userId AND m.role = 'OWNER'"
                + " AND g.deleted_at > :cutoff")
        .param(GROUP_ID, groupId)
        .param(USER_ID, userId)
        .param(CUTOFF, Timestamp.from(cutoff))
        .query(GroupSummary.class)
        .optional();
  }

  /** Takes the Group out of the Group Trash. */
  public void restore(UUID groupId) {
    jdbc.sql("UPDATE contact_group SET deleted_at = NULL WHERE id = :groupId")
        .param(GROUP_ID, groupId)
        .update();
  }

  /**
   * Locks the Groups of which the User is an Owner, in the order of their ids, until the end of the
   * transaction: the check of the Owners then cannot race with a change of Roles (R-021-02).
   */
  @GlobalScope
  public void lockGroupsOwnedBy(UUID userId) {
    jdbc.sql(
            "SELECT g.id FROM contact_group g JOIN member m ON m.group_id = g.id"
                + " WHERE m.user_id = :userId AND m.role = 'OWNER' ORDER BY g.id FOR UPDATE OF g")
        .param(USER_ID, userId)
        .query()
        .rowSet();
  }

  /**
   * The Groups of which the User is the only Owner, in Trash for less than 30 days or not (D-27 A1),
   * ordered by name (R-021-02).
   */
  @GlobalScope
  public List<SoleOwnerGroup> findSoleOwned(UUID userId, Instant cutoff, Instant now) {
    return jdbc.sql(
            "SELECT g.id, g.name, g.deleted_at FROM contact_group g"
                + " JOIN member m ON m.group_id = g.id"
                + " WHERE m.user_id = :userId AND m.role = 'OWNER'"
                + " AND (g.deleted_at IS NULL OR g.deleted_at > :cutoff)"
                + " AND NOT EXISTS (SELECT 1 FROM member o WHERE o.group_id = g.id"
                + " AND o.role = 'OWNER' AND o.user_id <> :userId)"
                + " ORDER BY lower(g.name), g.id")
        .param(USER_ID, userId)
        .param(CUTOFF, Timestamp.from(cutoff))
        .query(
            (rs, row) -> {
              Timestamp deletedAt = rs.getTimestamp("deleted_at");
              return new SoleOwnerGroup(
                  rs.getObject("id", UUID.class),
                  rs.getString("name"),
                  deletedAt != null,
                  deletedAt == null ? null : TrashPeriod.daysLeft(deletedAt.toInstant(), now));
            })
        .list();
  }

  /** The Groups that have been in the Group Trash since {@code cutoff} or before (R-020-05). */
  @GlobalScope
  public List<UUID> findExpired(Instant cutoff) {
    return jdbc.sql("SELECT id FROM contact_group WHERE deleted_at <= :cutoff")
        .param(CUTOFF, Timestamp.from(cutoff))
        .query(UUID.class)
        .list();
  }

  /**
   * Deletes the Group in Trash since {@code cutoff} or before; its Contacts, Tags, Attributes,
   * Versions, History, Members and Invitations go with it (ADR-03). False when it is gone or was
   * restored meanwhile.
   */
  @GlobalScope
  public boolean deleteExpired(UUID groupId, Instant cutoff) {
    return jdbc.sql("DELETE FROM contact_group WHERE id = :groupId AND deleted_at <= :cutoff")
            .param(GROUP_ID, groupId)
            .param(CUTOFF, Timestamp.from(cutoff))
            .update()
        > 0;
  }
}
