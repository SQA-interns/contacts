package si.contactgroups.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import si.contactgroups.domain.GroupSummary;
import si.contactgroups.domain.Role;
import si.contactgroups.persistence.GroupRepository;

/** Groups and the Group list (US-002). */
@Service
public class GroupService {

  private final GroupRepository groups;
  private final MembershipService memberships;
  private final Clock clock;

  public GroupService(GroupRepository groups, MembershipService memberships, Clock clock) {
    this.groups = groups;
    this.memberships = memberships;
    this.clock = clock;
  }

  public List<GroupSummary> listGroups(UUID userId) {
    return groups.findGroupsOfUser(userId);
  }

  /** Creates a Group with the trimmed name and makes the User its Owner (R-002-01). */
  @Transactional
  public GroupSummary createGroup(UUID userId, String name) {
    String trimmed = name.strip();
    Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
    UUID groupId = groups.insert(trimmed, now);
    memberships.addCreatorAsOwner(groupId, userId, now);
    return new GroupSummary(groupId, trimmed, Role.OWNER, now);
  }
}
