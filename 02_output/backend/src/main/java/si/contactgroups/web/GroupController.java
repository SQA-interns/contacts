package si.contactgroups.web;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import si.contactgroups.domain.GroupSummary;
import si.contactgroups.security.AccountEndpoint;
import si.contactgroups.security.AuthenticatedUser;
import si.contactgroups.service.GroupService;

/** The Group list and creating a Group (contract {@code listGroups}, {@code createGroup}). */
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

  @PostMapping
  @AccountEndpoint
  @ResponseStatus(HttpStatus.CREATED)
  public GroupSummary createGroup(
      @AuthenticationPrincipal AuthenticatedUser caller, @RequestBody GroupCreate body) {
    return groups.createGroup(caller.userId(), body.name());
  }

  /** Contract schema {@code GroupCreate}. */
  public record GroupCreate(String name) {}
}
