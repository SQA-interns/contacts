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
import si.contactgroups.domain.DuplicateSet;
import si.contactgroups.security.AuthenticatedUser;
import si.contactgroups.security.RequiresAction;
import si.contactgroups.service.DuplicateService;

/** Contracts {@code findDuplicates} (US-024) and {@code dismissDuplicate} (US-025). */
@RestController
@RequestMapping("/api/v1/groups/{groupId}/duplicates")
public class DuplicateController {

  private final DuplicateService duplicates;

  public DuplicateController(DuplicateService duplicates) {
    this.duplicates = duplicates;
  }

  @GetMapping
  @RequiresAction(Action.FIND_DUPLICATES)
  public List<DuplicateSet> findDuplicates(@PathVariable UUID groupId) {
    return duplicates.find(groupId);
  }

  @PostMapping("/dismissals")
  @RequiresAction(Action.DISMISS_DUPLICATE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void dismissDuplicate(
      @AuthenticationPrincipal AuthenticatedUser caller,
      @PathVariable UUID groupId,
      @RequestBody DuplicateDismissal body) {
    duplicates.dismiss(groupId, caller.userId(), body.contactIds());
  }

  /** Contract schema {@code DuplicateDismissal}. */
  public record DuplicateDismissal(List<UUID> contactIds) {}
}
