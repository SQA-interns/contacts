package si.contactgroups.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import si.contactgroups.domain.GroupTrashItem;
import si.contactgroups.domain.Role;
import si.contactgroups.domain.User;

/** US-020: delete a Group into the Group Trash and list it (R-020-01..04). */
class GroupTrashServiceIT extends GroupTrashTestSupport {

  @Test
  void r_020_01_typed_name_moves_the_group_out_of_the_group_list() {
    User ana = newUser();
    UUID acme = newGroup(ana, "Acme");

    groups.trashGroup(acme, "Acme");

    assertThat(groupListOf(ana)).doesNotContain(acme);
  }

  @Test
  void ex_020_01_typed_name_is_trimmed() {
    User ana = newUser();
    UUID acme = newGroup(ana, "Acme");

    groups.trashGroup(acme, "  Acme  ");

    assertThat(groupListOf(ana)).doesNotContain(acme);
  }

  @Test
  void r_020_02_a_name_in_other_case_is_refused_and_the_group_stays() {
    User ana = newUser();
    UUID acme = newGroup(ana, "Acme");

    assertThatThrownBy(() -> groups.trashGroup(acme, "acme"))
        .isInstanceOf(GroupNameMismatchException.class);
    assertThatThrownBy(() -> groups.trashGroup(acme, null))
        .isInstanceOf(GroupNameMismatchException.class);

    assertThat(groupListOf(ana)).contains(acme);
  }

  @Test
  void r_020_04_the_owner_sees_the_group_in_the_group_trash_with_30_days_left() {
    User ana = newUser();
    UUID acme = newGroup(ana, "Acme");
    groups.trashGroup(acme, "Acme");

    List<GroupTrashItem> trash = groups.listGroupTrash(ana.id());

    assertThat(trash).hasSize(1);
    assertThat(trash.get(0).id()).isEqualTo(acme);
    assertThat(trash.get(0).name()).isEqualTo("Acme");
    assertThat(trash.get(0).daysLeft()).isEqualTo(30);
  }

  @Test
  void r_020_04_ex_020_03_every_owner_sees_it_and_an_editor_does_not() {
    User ana = newUser();
    User cene = newUser();
    User boris = newUser();
    UUID acme = newGroup(ana, "Acme");
    addMember(acme, cene, Role.OWNER);
    addMember(acme, boris, Role.EDITOR);
    groups.trashGroup(acme, "Acme");

    assertThat(groups.listGroupTrash(cene.id()))
        .extracting(GroupTrashItem::id)
        .containsExactly(acme);
    assertThat(groups.listGroupTrash(boris.id())).isEmpty();
  }

  @Test
  void r_020_04_a_group_that_is_not_in_trash_is_not_listed() {
    User ana = newUser();
    newGroup(ana, "Acme");

    assertThat(groups.listGroupTrash(ana.id())).isEmpty();
  }

  @Test
  void r_020_04_days_left_count_a_started_day_as_one() {
    User ana = newUser();
    UUID acme = newGroup(ana, "Acme");
    groups.trashGroup(acme, "Acme");
    trashedAgo(acme, Duration.ofDays(29).plusHours(23));

    assertThat(groups.listGroupTrash(ana.id()))
        .extracting(GroupTrashItem::daysLeft)
        .containsExactly(1);
  }
}
