package si.contactgroups.web;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import si.contactgroups.domain.Action;
import si.contactgroups.domain.GroupSummary;
import si.contactgroups.domain.GroupTrashItem;
import si.contactgroups.security.AccountEndpoint;
import si.contactgroups.security.AuthenticatedUser;
import si.contactgroups.security.RequiresAction;
import si.contactgroups.service.GroupNotFoundException;
import si.contactgroups.service.GroupService;

/**
 * The Group list, creating a Group, and the Group Trash (contract {@code listGroups}, {@code
 * createGroup}, {@code trashGroup}, {@code listGroupTrash}, {@code restoreGroup}).
 */
@RestController
@RequestMapping("/api/v1")
public class GroupController {

  private final GroupService groups;

  public GroupController(GroupService groups) {
    this.groups = groups;
  }

  @GetMapping("/groups")
  @AccountEndpoint
  public List<GroupSummary> listGroups(@AuthenticationPrincipal AuthenticatedUser caller) {
    return groups.listGroups(caller.userId(), caller.email());
  }

  @PostMapping("/groups")
  @AccountEndpoint
  @ResponseStatus(HttpStatus.CREATED)
  public GroupSummary createGroup(
      @AuthenticationPrincipal AuthenticatedUser caller, @RequestBody GroupCreate body) {
    return groups.createGroup(caller.userId(), body.name());
  }

  @PostMapping("/groups/{groupId}/trash")
  @RequiresAction(Action.DELETE_GROUP)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void trashGroup(@PathVariable UUID groupId, @RequestBody GroupTrashRequest body) {
    groups.trashGroup(groupId, body.name());
  }

  /** Owner-only, so it checks its rule in the service (ADR-05). */
  @GetMapping("/group-trash")
  @AccountEndpoint
  public List<GroupTrashItem> listGroupTrash(@AuthenticationPrincipal AuthenticatedUser caller) {
    return groups.listGroupTrash(caller.userId());
  }

  /** The Group is in Trash, so the path variable is not named {@code groupId} (ADR-05). */
  @PostMapping("/group-trash/{trashedGroupId}/restore")
  @AccountEndpoint
  public GroupSummary restoreGroup(
      @AuthenticationPrincipal AuthenticatedUser caller, @PathVariable String trashedGroupId) {
    return groups.restoreGroup(caller.userId(), parse(trashedGroupId));
  }

  private static UUID parse(String id) {
    try {
      return UUID.fromString(id);
    } catch (IllegalArgumentException e) {
      throw new GroupNotFoundException();
    }
  }

  /** Contract schema {@code GroupCreate}. */
  public record GroupCreate(String name) {}

  /** Contract schema {@code GroupTrashRequest}. */
  public record GroupTrashRequest(String name) {}
}
