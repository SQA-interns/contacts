package si.contactgroups.web;

import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import si.contactgroups.domain.Action;
import si.contactgroups.security.RequiresAction;
import si.contactgroups.service.ContactNotFoundException;
import si.contactgroups.service.HistoryQueryService;
import si.contactgroups.service.PageNumber;

/**
 * The History of a Contact (contract {@code getContactHistory}). There is no way to change or
 * delete an entry: other methods answer 405 (R-016-05).
 */
@RestController
@RequestMapping("/api/v1/groups/{groupId}/contacts/{contactId}/history")
public class HistoryController {

  private final HistoryQueryService history;

  public HistoryController(HistoryQueryService history) {
    this.history = history;
  }

  @GetMapping
  @RequiresAction(Action.READ_HISTORY)
  public HistoryQueryService.Page getContactHistory(
      @PathVariable UUID groupId,
      @PathVariable String contactId,
      @RequestParam(required = false) String page) {
    int number = PageNumber.parse(page);
    UUID contact;
    try {
      contact = UUID.fromString(contactId);
    } catch (IllegalArgumentException e) {
      throw new ContactNotFoundException();
    }
    return history.contactHistory(groupId, contact, number);
  }
}
