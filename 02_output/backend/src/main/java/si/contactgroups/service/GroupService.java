package si.contactgroups.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import si.contactgroups.domain.GroupSummary;
import si.contactgroups.persistence.GroupRepository;

/** Groups and the Group list (US-002; the read-only slice of D-40). */
@Service
public class GroupService {

  private final GroupRepository groups;

  public GroupService(GroupRepository groups) {
    this.groups = groups;
  }

  public List<GroupSummary> listGroups(UUID userId) {
    return groups.findGroupsOfUser(userId);
  }
}
