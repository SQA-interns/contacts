package si.contactgroups.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import si.contactgroups.domain.HistoryAction;
import si.contactgroups.domain.HistoryActor;
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
                assertThat(e.groups()).containsExactly(new SoleOwnerGroup(acme, "Acme", true, 12)));
  }

  private int rows(String sql, UUID id) {
    return jdbc.sql(sql).param("id", id).query(Integer.class).single();
  }

  private UUID contactBy(UUID groupId, User creator) {
    return jdbc.sql(
            "INSERT INTO contact (group_id, last_name, created_by) VALUES (:g, 'Kontakt', :u)"
                + " RETURNING id")
        .param("g", groupId)
        .param("u", creator.id())
        .query(UUID.class)
        .single();
  }

  private void editedBy(UUID groupId, User user, UUID contactId) {
    history.insert(
        groupId,
        HistoryAction.CONTACT_EDITED,
        contactId,
        "{}",
        new HistoryActor(user.id(), user.displayName(), user.email(), Instant.now()));
  }

  @Test
  void r_021_01_the_user_the_memberships_and_the_firebase_account_are_deleted() {
    User ana = newUser();
    User boris = newUser();
    UUID acme = newGroup(ana, "Acme");
    addMember(acme, boris, Role.EDITOR);

    accounts.deleteAccount(boris.id());

    assertThat(userRepository.findById(boris.id())).isEmpty();
    assertThat(rows("SELECT count(*) FROM member WHERE user_id = :id", boris.id())).isZero();
    assertThat(directory.deleted).containsExactly(boris.firebaseUid());
    assertThat(members.exists(acme, ana.id())).isTrue();
  }

  @Test
  void ex_021_02_a_user_without_a_group_is_deleted() {
    User cene = newUser();

    accounts.deleteAccount(cene.id());

    assertThat(userRepository.findById(cene.id())).isEmpty();
    assertThat(directory.deleted).containsExactly(cene.firebaseUid());
  }

  @Test
  void r_021_01_a_second_owner_lets_the_sole_owner_delete_the_account() {
    User ana = newUser();
    User cene = newUser();
    UUID acme = newGroup(ana, "Acme");
    addMember(acme, cene, Role.OWNER);

    accounts.deleteAccount(ana.id());

    assertThat(userRepository.findById(ana.id())).isEmpty();
    assertThat(members.countOwners(acme)).isEqualTo(1);
  }

  @Test
  void r_021_01_a_failure_of_firebase_rolls_the_database_back() {
    User ana = newUser();
    User boris = newUser();
    UUID acme = newGroup(ana, "Acme");
    addMember(acme, boris, Role.EDITOR);
    UUID contact = contactBy(acme, boris);
    editedBy(acme, boris, contact);
    directory.failing = true;

    assertThatThrownBy(() -> accounts.deleteAccount(boris.id()))
        .isInstanceOf(IllegalStateException.class);

    assertThat(userRepository.findById(boris.id())).isPresent();
    assertThat(members.exists(acme, boris.id())).isTrue();
    assertThat(
            rows(
                "SELECT count(*) FROM history_entry WHERE user_id = :id AND user_email IS NOT NULL",
                boris.id()))
        .isEqualTo(1);
  }

  @Test
  void r_021_03_history_keeps_the_name_and_loses_the_email_of_the_user() {
    User ana = newUser();
    User boris = newUser();
    UUID acme = newGroup(ana, "Acme");
    addMember(acme, boris, Role.EDITOR);
    UUID contact = contactBy(acme, boris);
    editedBy(acme, boris, contact);
    editedBy(acme, ana, contact);

    accounts.deleteAccount(boris.id());

    assertThat(
            rows(
                "SELECT count(*) FROM history_entry WHERE user_id = :id"
                    + " AND user_email IS NULL AND user_name = 'Ana Novak'",
                boris.id()))
        .isEqualTo(1);
    assertThat(
            rows(
                "SELECT count(*) FROM history_entry WHERE user_id = :id AND user_email IS NOT NULL",
                ana.id()))
        .isEqualTo(1);
  }

  @Test
  void r_021_04_the_contacts_the_user_created_stay() {
    User ana = newUser();
    User boris = newUser();
    UUID acme = newGroup(ana, "Acme");
    addMember(acme, boris, Role.EDITOR);
    UUID contact = contactBy(acme, boris);

    accounts.deleteAccount(boris.id());

    assertThat(rows("SELECT count(*) FROM contact WHERE id = :id", contact)).isEqualTo(1);
  }

  @Test
  void ex_021_04_a_pending_invitation_to_the_address_stays_pending() {
    User ana = newUser();
    User cene = newUser();
    UUID acme = newGroup(ana, "Acme");
    jdbc.sql(
            "INSERT INTO invitation (group_id, email, role, status, created_at, expires_at,"
                + " created_by) VALUES (:g, :email, 'VIEWER', 'PENDING', now(),"
                + " now() + interval '14 days', :by)")
        .param("g", acme)
        .param("email", cene.email())
        .param("by", ana.id())
        .update();

    accounts.deleteAccount(cene.id());

    assertThat(
            rows(
                "SELECT count(*) FROM invitation WHERE group_id = :id AND status = 'PENDING'",
                acme))
        .isEqualTo(1);
  }

  @Test
  void r_021_02_two_owners_deleting_at_once_never_leave_the_group_without_an_owner() {
    User ana = newUser();
    User cene = newUser();
    UUID acme = newGroup(ana, "Acme");
    addMember(acme, cene, Role.OWNER);
    CountDownLatch start = new CountDownLatch(1);

    List<CompletableFuture<Boolean>> runs =
        List.of(ana, cene).stream()
            .map(
                user ->
                    CompletableFuture.supplyAsync(
                        () -> {
                          await(start);
                          try {
                            accounts.deleteAccount(user.id());
                            return true;
                          } catch (SoleOwnerException e) {
                            return false;
                          }
                        }))
            .toList();
    start.countDown();

    assertThat(runs.stream().map(CompletableFuture::join).filter(done -> done).count())
        .isEqualTo(1);
    assertThat(members.countOwners(acme)).isEqualTo(1);
  }

  private static void await(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
