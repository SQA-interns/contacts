package si.contactgroups.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Every action of the permission matrix (BR-02, decision D-25) with the Roles that may do it. This
 * enum is the only place where the matrix lives (ADR-05).
 */
public enum Action {
  READ_CONTACTS(Role.OWNER, Role.EDITOR, Role.VIEWER),
  EDIT_CONTACT(Role.OWNER, Role.EDITOR),
  MANAGE_TAGS(Role.OWNER, Role.EDITOR),
  DELETE_CONTACT(Role.OWNER, Role.EDITOR),
  READ_TRASH(Role.OWNER, Role.EDITOR),
  RESTORE_CONTACT(Role.OWNER, Role.EDITOR),
  DELETE_CONTACT_FOREVER(Role.OWNER),
  EXPORT(Role.OWNER, Role.EDITOR),
  READ_HISTORY(Role.OWNER, Role.EDITOR),
  READ_VERSIONS(Role.OWNER, Role.EDITOR),
  RESTORE_VERSION(Role.OWNER, Role.EDITOR),
  FIND_DUPLICATES(Role.OWNER, Role.EDITOR),
  DISMISS_DUPLICATE(Role.OWNER, Role.EDITOR),
  MERGE_CONTACTS(Role.OWNER, Role.EDITOR),
  MANAGE_MEMBERS(Role.OWNER),
  READ_MEMBERS(Role.OWNER, Role.EDITOR, Role.VIEWER),
  DELETE_GROUP(Role.OWNER);

  private final Set<Role> roles;

  Action(Role first, Role... more) {
    this.roles = EnumSet.of(first, more);
  }

  /** Whether a Member with this Role may do the action. */
  public boolean isAllowedFor(Role role) {
    return roles.contains(role);
  }

  /** The Roles that may do the action (a copy). */
  public Set<Role> allowedRoles() {
    return EnumSet.copyOf(roles);
  }
}
