package si.contactgroups.web;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import si.contactgroups.domain.Action;
import si.contactgroups.domain.Member;
import si.contactgroups.domain.Role;
import si.contactgroups.error.ApiError.FieldError;
import si.contactgroups.security.AuthenticatedUser;
import si.contactgroups.security.RequiresAction;
import si.contactgroups.service.MemberNotFoundException;
import si.contactgroups.service.MembershipService;
import si.contactgroups.service.ValidationException;

/** Contract {@code listMembers}, {@code changeMemberRole}, {@code removeMember}. */
@RestController
@RequestMapping("/api/v1/groups/{groupId}/members")
public class MemberController {

  private final MembershipService memberships;

  public MemberController(MembershipService memberships) {
    this.memberships = memberships;
  }

  @GetMapping
  @RequiresAction(Action.READ_MEMBERS)
  public List<Member> listMembers(@PathVariable UUID groupId) {
    return memberships.list(groupId);
  }

  @PatchMapping("/{userId}")
  @RequiresAction(Action.MANAGE_MEMBERS)
  public Member changeMemberRole(
      @AuthenticationPrincipal AuthenticatedUser caller,
      @PathVariable UUID groupId,
      @PathVariable String userId,
      @RequestBody RoleChange body) {
    if (body.role() == null) {
      throw new ValidationException(List.of(new FieldError("role", "validation.required")));
    }
    return memberships.changeRole(groupId, caller.userId(), parse(userId), body.role());
  }

  @DeleteMapping("/{userId}")
  @RequiresAction(Action.MANAGE_MEMBERS)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeMember(
      @AuthenticationPrincipal AuthenticatedUser caller,
      @PathVariable UUID groupId,
      @PathVariable String userId) {
    memberships.remove(groupId, caller.userId(), parse(userId));
  }

  private static UUID parse(String id) {
    try {
      return UUID.fromString(id);
    } catch (IllegalArgumentException e) {
      throw new MemberNotFoundException();
    }
  }

  /** Contract schema {@code RoleChange}. */
  public record RoleChange(Role role) {}
}
