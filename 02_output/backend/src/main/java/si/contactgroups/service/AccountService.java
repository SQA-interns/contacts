package si.contactgroups.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import si.contactgroups.domain.SoleOwnerGroup;
import si.contactgroups.domain.TrashPeriod;
import si.contactgroups.persistence.GroupRepository;
import si.contactgroups.persistence.HistoryRepository;
import si.contactgroups.persistence.MemberRepository;
import si.contactgroups.persistence.UserRepository;
import si.contactgroups.security.AccountDirectory;

/** Deletes the account of the signed-in User (US-021). */
@Service
public class AccountService {

  private final UserRepository users;
  private final MemberRepository members;
  private final GroupRepository groups;
  private final HistoryRepository history;
  private final AccountDirectory directory;
  private final Clock clock;
  private final TransactionTemplate tx;

  public AccountService(
      UserRepository users,
      MemberRepository members,
      GroupRepository groups,
      HistoryRepository history,
      AccountDirectory directory,
      Clock clock,
      PlatformTransactionManager transactions) {
    this.users = users;
    this.members = members;
    this.groups = groups;
    this.history = history;
    this.directory = directory;
    this.clock = clock;
    this.tx = new TransactionTemplate(transactions);
  }

  /**
   * Deletes the Memberships and the User, removes the e-mail address from the History entries the
   * User caused and then deletes the Firebase account; a failure of Firebase rolls the database
   * back. Contacts and Invitations stay (R-021-01, R-021-03, R-021-04, D-27 B1).
   *
   * @throws SoleOwnerException when the User is the only Owner of a Group, also one in the Group
   *     Trash; nothing is deleted (R-021-02, D-27 A1)
   * @throws IllegalStateException when Firebase cannot delete the account
   */
  public void deleteAccount(UUID userId) {
    tx.executeWithoutResult(status -> delete(userId));
  }

  private void delete(UUID userId) {
    users.lock(userId);
    refuseSoleOwner(userId);
    String firebaseUid = users.findById(userId).orElseThrow().firebaseUid();
    members.deleteAllOf(userId);
    history.clearEmailOf(userId);
    users.delete(userId);
    directory.delete(firebaseUid);
  }

  private void refuseSoleOwner(UUID userId) {
    groups.lockGroupsOwnedBy(userId);
    Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
    List<SoleOwnerGroup> sole = groups.findSoleOwned(userId, TrashPeriod.cutoff(now), now);
    if (!sole.isEmpty()) {
      throw new SoleOwnerException(sole);
    }
  }
}
