package si.contactgroups.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import si.contactgroups.domain.Role;
import si.contactgroups.domain.SoleOwnerGroup;
import si.contactgroups.domain.User;
import si.contactgroups.persistence.GroupRepository;
import si.contactgroups.persistence.HistoryRepository;
import si.contactgroups.persistence.MemberRepository;
import si.contactgroups.persistence.UserRepository;
import si.contactgroups.security.AccountDirectory;

/** US-021: delete my account (R-021-01..04) against the real database and a fake Firebase. */
class AccountServiceIT extends GroupTrashTestSupport {

  @Autowired private UserRepository userRepository;
  @Autowired private MemberRepository members;
  @Autowired private GroupRepository groupRepository;
  @Autowired private HistoryRepository history;
  @Autowired private Clock clock;
  @Autowired private PlatformTransactionManager transactions;

  private FakeDirectory directory;
  private AccountService accounts;

  /** Records the accounts deleted in Firebase; can be told to fail. */
  private static final class FakeDirectory implements AccountDirectory {
    private final List<String> deleted = new ArrayList<>();
    private boolean failing;

    @Override
    public void delete(String firebaseUid) {
      if (failing) {
        throw new IllegalStateException("Firebase is down");
      }
      deleted.add(firebaseUid);
    }

    @Override
    public boolean exists(String firebaseUid) {
      return !deleted.contains(firebaseUid);
    }
  }

  @BeforeEach
  void service() {
    directory = new FakeDirectory();
    accounts =
        new AccountService(
            userRepository, members, groupRepository, history, directory, clock, transactions);
  }

  @Test
  void r_021_02_the_sole_owner_of_a_group_gets_it_listed_and_nothing_is_deleted() {
    User ana = newUser();
    UUID acme = newGroup(ana, "Acme");

    assertThatThrownBy(() -> accounts.deleteAccount(ana.id()))
        .isInstanceOfSatisfying(
            SoleOwnerException.class,
            e ->
                assertThat(e.groups())
                    .containsExactly(new SoleOwnerGroup(acme, "Acme", false, null)));

    assertThat(userRepository.findById(ana.id())).isPresent();
    assertThat(directory.deleted).isEmpty();
  }

  @Test
  void ex_021_01_a_sole_owner_of_two_groups_gets_both_listed_by_name() {
    User ana = newUser();
    UUID beta = newGroup(ana, "beta");
    UUID acme = newGroup(ana, "Acme");

    assertThatThrownBy(() -> accounts.deleteAccount(ana.id()))
        .isInstanceOfSatisfying(
            SoleOwnerException.class,
            e -> assertThat(e.groups()).extracting(SoleOwnerGroup::id).containsExactly(acme, beta));
  }

  @Test
  void r_021_02_a_group_with_a_second_owner_or_where_the_user_is_no_owner_is_not_listed() {
    User ana = newUser();
    User cene = newUser();
    User boris = newUser();
    UUID shared = newGroup(ana, "Shared");
    addMember(shared, cene, Role.OWNER);
    UUID others = newGroup(boris, "Others");
    addMember(others, ana, Role.EDITOR);
    UUID mine = newGroup(ana, "Mine");

    assertThatThrownBy(() -> accounts.deleteAccount(ana.id()))
        .isInstanceOfSatisfying(
            SoleOwnerException.class,
            e -> assertThat(e.groups()).extracting(SoleOwnerGroup::id).containsExactly(mine));
  }

  @Test
  void d_27_a_group_in_the_group_trash_counts_and_shows_the_days_left() {
    User ana = newUser();
    UUID acme = newGroup(ana, "Acme");
    trashedAgo(acme, Duration.ofDays(18).plusHours(1));

    assertThatThrownBy(() -> accounts.deleteAccount(ana.id()))
        .isInstanceOfSatisfying(
            SoleOwnerException.class,
            e ->
                assertThat(e.groups())
                    .containsExactly(new SoleOwnerGroup(acme, "Acme", true, 12)));
  }
}
