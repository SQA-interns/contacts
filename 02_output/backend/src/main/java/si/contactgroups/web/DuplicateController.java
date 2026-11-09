package si.contactgroups.web;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import si.contactgroups.domain.Action;
import si.contactgroups.domain.DuplicateSet;
import si.contactgroups.security.RequiresAction;
import si.contactgroups.service.DuplicateService;

/** Contract {@code findDuplicates} (US-024); the search changes nothing. */
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
}
