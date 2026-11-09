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
import si.contactgroups.domain.Invitation;
import si.contactgroups.domain.Role;
import si.contactgroups.security.AuthenticatedUser;
import si.contactgroups.security.RequiresAction;
import si.contactgroups.service.InvitationNotFoundException;
import si.contactgroups.service.InvitationService;

/** Contract {@code listInvitations}, {@code createInvitation}, {@code cancelInvitation}. */
@RestController
@RequestMapping("/api/v1/groups/{groupId}/invitations")
public class InvitationController {

  private final InvitationService invitations;

  public InvitationController(InvitationService invitations) {
    this.invitations = invitations;
  }

  @GetMapping
  @RequiresAction(Action.MANAGE_MEMBERS)
  public List<Invitation> listInvitations(@PathVariable UUID groupId) {
    return invitations.listPending(groupId);
  }

  @PostMapping
  @RequiresAction(Action.MANAGE_MEMBERS)
  @ResponseStatus(HttpStatus.CREATED)
  public Invitation createInvitation(
      @AuthenticationPrincipal AuthenticatedUser caller,
      @PathVariable UUID groupId,
      @RequestBody InvitationCreate body) {
    return invitations.create(groupId, caller.userId(), body.email(), body.role());
  }

  @PostMapping("/{invitationId}/cancel")
  @RequiresAction(Action.MANAGE_MEMBERS)
  public Invitation cancelInvitation(
      @AuthenticationPrincipal AuthenticatedUser caller,
      @PathVariable UUID groupId,
      @PathVariable String invitationId) {
    return invitations.cancel(groupId, caller.userId(), parse(invitationId));
  }

  private static UUID parse(String id) {
    try {
      return UUID.fromString(id);
    } catch (IllegalArgumentException e) {
      throw new InvitationNotFoundException();
    }
  }

  /** Contract schema {@code InvitationCreate}. */
  public record InvitationCreate(String email, Role role) {}
}
