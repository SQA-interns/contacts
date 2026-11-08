package si.contactgroups.web;

import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import si.contactgroups.domain.GroupSummary;
import si.contactgroups.security.AccountEndpoint;
import si.contactgroups.security.AuthenticatedUser;
import si.contactgroups.service.GroupService;

/** The Group list (contract {@code listGroups}; the read-only slice of D-40). */
@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

  private final GroupService groups;

  public GroupController(GroupService groups) {
    this.groups = groups;
  }

  @GetMapping
  @AccountEndpoint
  public List<GroupSummary> listGroups(@AuthenticationPrincipal AuthenticatedUser caller) {
    return groups.listGroups(caller.userId());
  }
}
