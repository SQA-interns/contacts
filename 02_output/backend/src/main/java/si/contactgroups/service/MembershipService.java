package si.contactgroups.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import si.contactgroups.domain.Member;
import si.contactgroups.domain.Role;
import si.contactgroups.persistence.GroupRepository;
import si.contactgroups.persistence.MemberRepository;

/** Who is a Member of which Group, with which Role (glossary: Member). */
@Service
public class MembershipService {

  private final MemberRepository members;

  private final GroupRepository groups;
  private final HistoryWriter history;
  private final Clock clock;

  public MembershipService(
      MemberRepository members, GroupRepository groups, HistoryWriter history, Clock clock) {
    this.members = members;
    this.groups = groups;
    this.history = history;
    this.clock = clock;
  }

  /** Every Member of the Group (R-015-01). */
  public List<Member> list(UUID groupId) {
    return members.findAll(groupId);
  }

  /**
   * Gives the Member the Role; nothing happens when it is the same Role (EX-015-02).
   *
   * @throws MemberNotFoundException when the User is no Member of the Group
   * @throws LastOwnerException when the Group would have no Owner (R-015-04)
   */
  @Transactional
  public Member changeRole(UUID groupId, UUID ownerId, UUID userId, Role role) {
    groups.lock(groupId);
    Member member = members.find(groupId, userId).orElseThrow(MemberNotFoundException::new);
    if (member.role() == role) {
      return member;
    }
    if (member.role() == Role.OWNER && members.countOwners(groupId) <= 1) {
      throw new LastOwnerException();
    }
    members.updateRole(groupId, userId, role);
    history.roleChanged(
        groupId,
        ownerId,
        member.displayName(),
        member.email(),
        member.role().name(),
        role.name(),
        now());
    return new Member(
        member.userId(), member.displayName(), member.email(), role, member.joinedAt());
  }

  /**
   * Removes the Member; the Contacts and History entries stay (R-015-07).
   *
   * @throws MemberNotFoundException when the User is no Member of the Group
   * @throws LastOwnerException when the Member is the last Owner (R-015-04)
   */
  @Transactional
  public void remove(UUID groupId, UUID ownerId, UUID userId) {
    groups.lock(groupId);
    Member member = members.find(groupId, userId).orElseThrow(MemberNotFoundException::new);
    if (member.role() == Role.OWNER && members.countOwners(groupId) <= 1) {
      throw new LastOwnerException();
    }
    members.delete(groupId, userId);
    history.memberRemoved(
        groupId, ownerId, member.displayName(), member.email(), member.role().name(), now());
  }

  private Instant now() {
    return clock.instant().truncatedTo(ChronoUnit.MICROS);
  }

  /** Makes the User the Owner of a Group that has just been created (R-002-01). */
  void addCreatorAsOwner(UUID groupId, UUID userId, Instant joinedAt) {
    members.insert(groupId, userId, Role.OWNER, joinedAt);
  }
}
