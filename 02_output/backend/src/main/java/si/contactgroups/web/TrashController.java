package si.contactgroups.web;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import si.contactgroups.domain.Action;
import si.contactgroups.domain.Contact;
import si.contactgroups.domain.TrashContactView;
import si.contactgroups.security.AuthenticatedUser;
import si.contactgroups.security.RequiresAction;
import si.contactgroups.service.TrashService;

/** Contract {@code trashContact}, {@code listTrashContacts}, {@code restoreContact} (US-012). */
@RestController
@RequestMapping("/api/v1/groups/{groupId}")
public class TrashController {

  private final TrashService trash;

  public TrashController(TrashService trash) {
    this.trash = trash;
  }

  @PostMapping("/contacts/{contactId}/trash")
  @RequiresAction(Action.DELETE_CONTACT)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void trashContact(
      @AuthenticationPrincipal AuthenticatedUser caller,
      @PathVariable UUID groupId,
      @PathVariable String contactId) {
    trash.moveToTrash(groupId, caller.userId(), ContactIds.parse(contactId));
  }

  @GetMapping("/trash/contacts")
  @RequiresAction(Action.READ_TRASH)
  public List<TrashContactView> listTrashContacts(@PathVariable UUID groupId) {
    return trash.list(groupId);
  }

  @PostMapping("/trash/contacts/{contactId}/restore")
  @RequiresAction(Action.RESTORE_CONTACT)
  public Contact restoreContact(
      @AuthenticationPrincipal AuthenticatedUser caller,
      @PathVariable UUID groupId,
      @PathVariable String contactId) {
    return trash.restore(groupId, caller.userId(), ContactIds.parse(contactId));
  }
}
