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

  private final GroupRepository groups;
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
    this.groups = groups;
    this.clock = clock;
    this.tx = new TransactionTemplate(transactions);
  }

  /**
   * Deletes the account of the User.
   *
   * @throws SoleOwnerException when the User is the only Owner of a Group, also one in the Group
   *     Trash; nothing is deleted (R-021-02, D-27 A1)
   */
  public void deleteAccount(UUID userId) {
    tx.executeWithoutResult(status -> refuseSoleOwner(userId));
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
