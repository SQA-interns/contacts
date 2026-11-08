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
import si.contactgroups.error.ApiError.FieldError;
import si.contactgroups.persistence.GroupRepository;
import si.contactgroups.persistence.UserRepository;

/** Groups and the Group list (US-002). */
@Service
public class GroupService {

  private static final int MAX_NAME_LENGTH = 100;

  private final GroupRepository groups;
  private final UserRepository users;
  private final MembershipService memberships;
  private final Clock clock;

  public GroupService(
      GroupRepository groups, UserRepository users, MembershipService memberships, Clock clock) {
    this.groups = groups;
    this.users = users;
    this.memberships = memberships;
    this.clock = clock;
  }

  public List<GroupSummary> listGroups(UUID userId) {
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
    Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
    UUID groupId = groups.insert(trimmed, now);
    memberships.addCreatorAsOwner(groupId, userId, now);
    return new GroupSummary(groupId, trimmed, Role.OWNER, now);
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
