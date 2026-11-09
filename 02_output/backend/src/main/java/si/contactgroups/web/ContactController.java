package si.contactgroups.web;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import si.contactgroups.domain.Action;
import si.contactgroups.domain.AttributePair;
import si.contactgroups.domain.Contact;
import si.contactgroups.domain.ContactData;
import si.contactgroups.domain.ContactPage;
import si.contactgroups.security.AuthenticatedUser;
import si.contactgroups.security.RequiresAction;
import si.contactgroups.service.ContactNotFoundException;
import si.contactgroups.service.ContactService;
import si.contactgroups.service.SearchService;

/**
 * Create, read and edit a Contact (contract {@code createContact}, {@code getContact}, {@code
 * updateContact}).
 */
@RestController
@RequestMapping("/api/v1/groups/{groupId}/contacts")
public class ContactController {

  private final ContactService contacts;
  private final SearchService search;

  public ContactController(ContactService contacts, SearchService search) {
    this.contacts = contacts;
    this.search = search;
  }

  @GetMapping
  @RequiresAction(Action.READ_CONTACTS)
  public ContactPage listContacts(
      @PathVariable UUID groupId,
      @RequestParam(required = false) String page,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) String dir) {
    return search.list(groupId, page, q, sort, dir);
  }

  @PostMapping
  @RequiresAction(Action.EDIT_CONTACT)
  @ResponseStatus(HttpStatus.CREATED)
  public Contact createContact(
      @AuthenticationPrincipal AuthenticatedUser caller,
      @PathVariable UUID groupId,
      @RequestBody ContactInput body) {
    return contacts.create(groupId, caller.userId(), body.toData(), body.attributes(), body.tags());
  }

  @GetMapping("/{contactId}")
  @RequiresAction(Action.READ_CONTACTS)
  public Contact getContact(@PathVariable UUID groupId, @PathVariable String contactId) {
    return contacts.get(groupId, contactIdOf(contactId));
  }

  @PutMapping("/{contactId}")
  @RequiresAction(Action.EDIT_CONTACT)
  public Contact updateContact(
      @AuthenticationPrincipal AuthenticatedUser caller,
      @PathVariable UUID groupId,
      @PathVariable String contactId,
      @RequestBody ContactInput body) {
    return contacts.update(
        groupId,
        caller.userId(),
        contactIdOf(contactId),
        body.version(),
        body.toData(),
        body.attributes(),
        body.tags());
  }

  private static UUID contactIdOf(String contactId) {
    try {
      return UUID.fromString(contactId);
    } catch (IllegalArgumentException e) {
      throw new ContactNotFoundException();
    }
  }

  /**
   * Contract schema {@code ContactInput}. A missing {@code attributes} or {@code tags} keeps the
   * stored ones on an edit.
   */
  public record ContactInput(
      String firstName,
      String lastName,
      String email,
      String phone,
      String company,
      String jobTitle,
      String street,
      String postalCode,
      String city,
      String country,
      String notes,
      List<String> tags,
      List<AttributePair> attributes,
      Integer version) {

    ContactData toData() {
      return new ContactData(
          firstName,
          lastName,
          email,
          phone,
          company,
          jobTitle,
          street,
          postalCode,
          city,
          country,
          notes);
    }
  }
}
