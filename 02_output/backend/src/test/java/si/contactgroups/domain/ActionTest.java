package si.contactgroups.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The permission table of docs/02_specification.md (BR-02, D-25) is the single source of truth. */
class ActionTest {

  private static final Set<Role> ALL = EnumSet.allOf(Role.class);
  private static final Set<Role> OWNER_EDITOR = EnumSet.of(Role.OWNER, Role.EDITOR);
  private static final Set<Role> OWNER_ONLY = EnumSet.of(Role.OWNER);

  private static final Map<Action, Set<Role>> EXPECTED =
      Map.ofEntries(
          Map.entry(Action.READ_CONTACTS, ALL),
          Map.entry(Action.EDIT_CONTACT, OWNER_EDITOR),
          Map.entry(Action.MANAGE_TAGS, OWNER_EDITOR),
          Map.entry(Action.DELETE_CONTACT, OWNER_EDITOR),
          Map.entry(Action.READ_TRASH, OWNER_EDITOR),
          Map.entry(Action.RESTORE_CONTACT, OWNER_EDITOR),
          Map.entry(Action.DELETE_CONTACT_FOREVER, OWNER_ONLY),
          Map.entry(Action.EXPORT, OWNER_EDITOR),
          Map.entry(Action.READ_HISTORY, OWNER_EDITOR),
          Map.entry(Action.READ_VERSIONS, OWNER_EDITOR),
          Map.entry(Action.RESTORE_VERSION, OWNER_EDITOR),
          Map.entry(Action.FIND_DUPLICATES, OWNER_EDITOR),
          Map.entry(Action.DISMISS_DUPLICATE, OWNER_EDITOR),
          Map.entry(Action.MERGE_CONTACTS, OWNER_EDITOR),
          Map.entry(Action.MANAGE_MEMBERS, OWNER_ONLY),
          Map.entry(Action.READ_MEMBERS, ALL),
          Map.entry(Action.DELETE_GROUP, OWNER_ONLY));

  @Test
  void everyActionHasAnExpectedRow() {
    assertThat(EXPECTED.keySet()).containsExactlyInAnyOrder(Action.values());
  }

  @Test
  void rolesPerActionMatchThePermissionTable() {
    for (Action action : Action.values()) {
      assertThat(action.allowedRoles()).as(action.name()).isEqualTo(EXPECTED.get(action));
    }
  }

  @Test
  void viewerCanOnlyReadContactsAndMembers() {
    for (Action action : Action.values()) {
      boolean readOnly = action == Action.READ_CONTACTS || action == Action.READ_MEMBERS;
      assertThat(action.isAllowedFor(Role.VIEWER)).as(action.name()).isEqualTo(readOnly);
    }
  }

  @Test
  void ownerMayDoEverything() {
    for (Action action : Action.values()) {
      assertThat(action.isAllowedFor(Role.OWNER)).as(action.name()).isTrue();
    }
  }
}
