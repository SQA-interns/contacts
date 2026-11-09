package si.contactgroups.web;

import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import si.contactgroups.domain.Action;
import si.contactgroups.security.RequiresAction;
import si.contactgroups.service.HistoryQueryService;

/** The Group activity list (contract {@code getGroupActivity}, US-017). */
@RestController
@RequestMapping("/api/v1/groups/{groupId}/activity")
public class ActivityController {

  private final HistoryQueryService history;

  public ActivityController(HistoryQueryService history) {
    this.history = history;
  }

  @GetMapping
  @RequiresAction(Action.READ_HISTORY)
  public HistoryQueryService.Page getGroupActivity(
      @PathVariable UUID groupId,
      @RequestParam(required = false) String userId,
      @RequestParam(required = false) String action,
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to,
      @RequestParam(defaultValue = "1") int page) {
    return history.groupActivity(groupId, userId, action, from, to, page);
  }
}
