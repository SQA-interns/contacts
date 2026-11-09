package si.contactgroups.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import si.contactgroups.domain.GroupSummary;
import si.contactgroups.domain.GroupTrashItem;
import si.contactgroups.domain.Role;
import si.contactgroups.domain.User;

/** US-020: restore a Group from the Group Trash (R-020-07, R-020-08). */
class GroupRestoreServiceIT extends GroupTrashTestSupport {

  @Test
  void r_020_07_ex_020_05_an_owner_restores_a_group_after_29_days_23_hours() {
    User ana = newUser();
    User boris = newUser();
    UUID acme = newGroup(ana, "Acme");
    addMember(acme, boris, Role.EDITOR);
    groups.trashGroup(acme, "Acme");
    trashedAgo(acme, Duration.ofDays(29).plusHours(23));

    GroupSummary restored = groups.restoreGroup(ana.id(), acme);

    assertThat(restored.id()).isEqualTo(acme);
    assertThat(restored.name()).isEqualTo("Acme");
    assertThat(restored.role()).isEqualTo(Role.OWNER);
    assertThat(groupListOf(ana)).contains(acme);
    assertThat(groupListOf(boris)).contains(acme);
    assertThat(groups.listGroupTrash(ana.id())).isEmpty();
  }

  @Test
  void r_020_07_ex_020_06_a_group_in_trash_for_30_days_is_not_found() {
    User ana = newUser();
    UUID acme = newGroup(ana, "Acme");
    groups.trashGroup(acme, "Acme");
    trashedAgo(acme, Duration.ofDays(30));

    assertThatThrownBy(() -> groups.restoreGroup(ana.id(), acme))
        .isInstanceOf(GroupNotFoundException.class);
    assertThat(groups.listGroupTrash(ana.id())).isEmpty();
  }

  @Test
  void r_020_07_ex_020_07_only_an_owner_of_that_group_restores_it() {
    User ana = newUser();
    User boris = newUser();
    User stranger = newUser();
    UUID acme = newGroup(ana, "Acme");
    addMember(acme, boris, Role.EDITOR);
    groups.trashGroup(acme, "Acme");

    assertThatThrownBy(() -> groups.restoreGroup(boris.id(), acme))
        .isInstanceOf(GroupNotFoundException.class);
    assertThatThrownBy(() -> groups.restoreGroup(stranger.id(), acme))
        .isInstanceOf(GroupNotFoundException.class);
    assertThat(groups.listGroupTrash(ana.id())).hasSize(1);
  }

  @Test
  void r_020_07_a_group_that_is_not_in_trash_cannot_be_restored() {
    User ana = newUser();
    UUID acme = newGroup(ana, "Acme");

    assertThatThrownBy(() -> groups.restoreGroup(ana.id(), acme))
        .isInstanceOf(GroupNotFoundException.class);
  }

  @Test
  void r_020_08_restore_is_refused_when_the_owner_has_a_group_of_the_same_name() {
    User ana = newUser();
    UUID acme = newGroup(ana, "Acme");
    groups.trashGroup(acme, "Acme");
    newGroup(ana, "ACME");

    assertThatThrownBy(() -> groups.restoreGroup(ana.id(), acme))
        .isInstanceOf(GroupNameTakenException.class);
    assertThat(groups.listGroupTrash(ana.id()))
        .extracting(GroupTrashItem::id)
        .containsExactly(acme);
  }

  @Test
  void r_020_08_a_group_of_the_same_name_in_trash_does_not_block_the_restore() {
    User ana = newUser();
    UUID first = newGroup(ana, "Acme");
    groups.trashGroup(first, "Acme");
    UUID second = newGroup(ana, "acme");
    groups.trashGroup(second, "acme");

    assertThat(groups.restoreGroup(ana.id(), first).id()).isEqualTo(first);
  }
}
