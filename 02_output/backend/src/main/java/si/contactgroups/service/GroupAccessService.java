package si.contactgroups.service;

import java.util.UUID;
import org.springframework.stereotype.Service;
import si.contactgroups.domain.Action;
import si.contactgroups.domain.Role;
import si.contactgroups.persistence.MemberRepository;

/**
 * Decides whether a User may do an {@link Action} in a Group: Membership first (404), then the Role
 * (403). The Role is read from the database on every call (SR-03, ADR-05).
 */
@Service
public class GroupAccessService {

  private final MemberRepository members;

  public GroupAccessService(MemberRepository members) {
    this.members = members;
  }

  /**
   * @param groupId the Group id as written in the request path
   * @throws GroupNotFoundException when the id is not a Group id, the Group does not exist or is in
   *     Trash, or the User is no Member of it
   * @throws RoleForbiddenException when the Role of the Member does not allow the action
   */
  public Role requireAllowed(String groupId, UUID userId, Action action) {
    Role role = members.findRole(parse(groupId), userId).orElseThrow(GroupNotFoundException::new);
    if (!action.isAllowedFor(role)) {
      throw new RoleForbiddenException();
    }
    return role;
  }

  private static UUID parse(String groupId) {
    try {
      return UUID.fromString(groupId);
    } catch (IllegalArgumentException e) {
      throw new GroupNotFoundException();
    }
  }
}
