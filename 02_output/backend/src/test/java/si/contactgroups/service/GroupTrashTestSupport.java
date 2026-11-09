package si.contactgroups.service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import si.contactgroups.domain.GroupSummary;
import si.contactgroups.domain.Role;
import si.contactgroups.domain.User;
import si.contactgroups.security.VerifiedToken;
import si.contactgroups.support.ApplicationContextSupport;

/** Set-up shared by the integration tests of US-020 (Group Trash). */
abstract class GroupTrashTestSupport extends ApplicationContextSupport {

  @Autowired protected GroupService groups;
  @Autowired protected UserService users;
  @Autowired protected JdbcClient jdbc;

  protected User newUser() {
    String uid = "uid-" + UUID.randomUUID();
    return users.findOrCreate(new VerifiedToken(uid, uid + "@acme.com", true, "Ana Novak"));
  }

  protected UUID newGroup(User owner, String name) {
    return groups.createGroup(owner.id(), name).id();
  }

  protected void addMember(UUID groupId, User user, Role role) {
    jdbc.sql(
            "INSERT INTO member (group_id, user_id, role, joined_at)"
                + " VALUES (:g, :u, :r, now())")
        .param("g", groupId)
        .param("u", user.id())
        .param("r", role.name())
        .update();
  }

  /** Puts the Group into Trash as if it had been deleted {@code ago} ago. */
  protected void trashedAgo(UUID groupId, Duration ago) {
    jdbc.sql("UPDATE contact_group SET deleted_at = :t WHERE id = :g")
        .param("t", Timestamp.from(Instant.now().minus(ago)))
        .param("g", groupId)
        .update();
  }

  protected List<UUID> groupListOf(User user) {
    return groups.listGroups(user.id(), user.email()).stream().map(GroupSummary::id).toList();
  }

  protected int count(String table, UUID groupId) {
    String column = "contact_group".equals(table) ? "id" : "group_id";
    return jdbc.sql("SELECT count(*) FROM " + table + " WHERE " + column + " = :g")
        .param("g", groupId)
        .query(Integer.class)
        .single();
  }
}
