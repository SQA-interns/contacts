package si.contactgroups.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import si.contactgroups.domain.GroupSummary;
import si.contactgroups.domain.GroupTrashItem;
import si.contactgroups.domain.Role;
import si.contactgroups.domain.TrashPeriod;
import si.contactgroups.error.ApiError.FieldError;
import si.contactgroups.persistence.GroupRepository;
import si.contactgroups.persistence.UserRepository;

/** Groups, the Group list (US-002) and the Group Trash (US-020). */
@Service
public class GroupService {

  private static final Logger LOG = LoggerFactory.getLogger(GroupService.class);
  private static final int MAX_NAME_LENGTH = 100;

  private final GroupRepository groups;
  private final UserRepository users;
  private final MembershipService memberships;
  private final InvitationService invitations;
  private final Clock clock;
  private final TransactionTemplate tx;

  public GroupService(
      GroupRepository groups,
      UserRepository users,
      MembershipService memberships,
      InvitationService invitations,
      Clock clock,
      PlatformTransactionManager transactions) {
    this.tx = new TransactionTemplate(transactions);
    this.invitations = invitations;
    this.groups = groups;
    this.users = users;
    this.memberships = memberships;
    this.clock = clock;
  }

  /**
   * The Groups of the User. Pending Invitations for the verified address of the User are accepted
   * first (R-014-05).
   */
  @Transactional
  public List<GroupSummary> listGroups(UUID userId, String email) {
    invitations.acceptPendingFor(userId, email);
    return groups.findGroupsOfUser(userId);
  }

  /**
   * Creates a Group with the trimmed name and makes the User its Owner (R-002-01). The row of the
   * User is locked first, so that two requests of the same User cannot both pass the name check.
   *
   * @throws ValidationException when the name breaks R-002-02
   * @throws GroupNameTakenException when the User owns a Group of that name (R-002-03)
   */
  @Transactional
  public GroupSummary createGroup(UUID userId, String name) {
    String trimmed = validName(name);
    users.lock(userId);
    if (groups.ownerHasGroupNamed(userId, trimmed)) {
      throw new GroupNameTakenException();
    }
    Instant now = now();
    UUID groupId = groups.insert(trimmed, now);
    memberships.addCreatorAsOwner(groupId, userId, now);
    return new GroupSummary(groupId, trimmed, Role.OWNER, now);
  }

  /**
   * Moves the Group to the Group Trash when the typed name equals the Group name after trimming,
   * same upper and lower case (R-020-01, R-020-02). The caller is an Owner of the Group.
   *
   * @throws GroupNameMismatchException when the typed name differs; nothing is stored
   * @throws GroupNotFoundException when the Group is already in Trash or gone
   */
  @Transactional
  public void trashGroup(UUID groupId, String typedName) {
    String name = groups.activeName(groupId).orElseThrow(GroupNotFoundException::new);
    if (typedName == null || !name.equals(typedName.strip())) {
      throw new GroupNameMismatchException();
    }
    if (!groups.moveToTrash(groupId, now())) {
      throw new GroupNotFoundException();
    }
  }

  /** The Groups in Trash of which the User is an Owner, with the days left (R-020-04). */
  public List<GroupTrashItem> listGroupTrash(UUID userId) {
    Instant now = now();
    return groups.findTrashOfOwner(userId, TrashPeriod.cutoff(now), now);
  }

  /**
   * Returns the Group from the Group Trash to the Group lists of its Members with everything in it
   * (R-020-07). The row of the User is locked first, as in {@link #createGroup}.
   *
   * @throws GroupNotFoundException when the Group is not in Trash, has been for 30 days, or the
   *     User is not an Owner of it
   * @throws GroupNameTakenException when the User owns a Group not in Trash with the same name
   *     ignoring case (R-020-08)
   */
  @Transactional
  public GroupSummary restoreGroup(UUID userId, UUID groupId) {
    users.lock(userId);
    GroupSummary group =
        groups
            .findRestorable(groupId, userId, TrashPeriod.cutoff(now()))
            .orElseThrow(GroupNotFoundException::new);
    if (groups.ownerHasGroupNamed(userId, group.name())) {
      throw new GroupNameTakenException();
    }
    groups.restore(groupId);
    return group;
  }

  /**
   * Deletes every Group that has been in the Group Trash for 30 days with all its data, each in its
   * own transaction so that one failure does not stop the others (R-020-05, ADR-09).
   *
   * @return the number of Groups deleted
   */
  public int purgeExpired() {
    Instant cutoff = TrashPeriod.cutoff(now());
    int deleted = 0;
    for (UUID groupId : groups.findExpired(cutoff)) {
      try {
        if (Boolean.TRUE.equals(tx.execute(s -> groups.deleteExpired(groupId, cutoff)))) {
          deleted++;
        }
      } catch (RuntimeException e) {
        LOG.warn("Group Trash purge of one Group failed: {}", e.getClass().getSimpleName());
      }
    }
    return deleted;
  }

  private Instant now() {
    return clock.instant().truncatedTo(ChronoUnit.MICROS);
  }

  /** The trimmed name, which must have 1 to 100 characters (R-002-02). */
  private static String validName(String name) {
    String trimmed = name == null ? "" : name.strip();
    if (trimmed.isEmpty() || trimmed.length() > MAX_NAME_LENGTH) {
      throw new ValidationException(List.of(new FieldError("name", "validation.name_invalid")));
    }
    return trimmed;
  }
}
