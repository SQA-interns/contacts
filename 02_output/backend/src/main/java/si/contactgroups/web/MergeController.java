package si.contactgroups.web;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import si.contactgroups.domain.Action;
import si.contactgroups.domain.AttributePair;
import si.contactgroups.domain.Contact;
import si.contactgroups.domain.ContactData;
import si.contactgroups.security.AuthenticatedUser;
import si.contactgroups.security.RequiresAction;
import si.contactgroups.service.MergeCommand;
import si.contactgroups.service.MergeService;

/** Contract {@code mergeContacts} (US-026); there is no merge without the user's request. */
@RestController
@RequestMapping("/api/v1/groups/{groupId}/merges")
public class MergeController {

  private final MergeService merges;

  public MergeController(MergeService merges) {
    this.merges = merges;
  }

  @PostMapping
  @RequiresAction(Action.MERGE_CONTACTS)
  public Contact mergeContacts(
      @AuthenticationPrincipal AuthenticatedUser caller,
      @PathVariable UUID groupId,
      @RequestBody MergeRequest body) {
    return merges.merge(groupId, caller.userId(), body.toCommand());
  }

  /**
   * Contract schema {@code MergeRequest}. The contract's {@code tags} is not read: the Survivor
   * always gets the Tags of all merged Contacts (R-026-03).
   */
  public record MergeRequest(
      UUID survivorId,
      List<MergeCommand.Target> contacts,
      ContactData fields,
      List<AttributePair> attributes,
      List<String> tags) {

    MergeCommand toCommand() {
      return new MergeCommand(survivorId, contacts, fields, attributes);
    }
  }
}
