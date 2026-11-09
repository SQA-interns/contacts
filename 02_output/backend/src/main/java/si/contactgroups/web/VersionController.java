package si.contactgroups.web;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import si.contactgroups.domain.Action;
import si.contactgroups.domain.Contact;
import si.contactgroups.domain.VersionComparison;
import si.contactgroups.domain.VersionDetail;
import si.contactgroups.domain.VersionSummary;
import si.contactgroups.security.AuthenticatedUser;
import si.contactgroups.security.RequiresAction;
import si.contactgroups.service.ContactNotFoundException;
import si.contactgroups.service.VersionService;

/**
 * The Versions of a Contact (contract {@code listVersions}, {@code getVersion}, {@code
 * compareVersion}, {@code restoreVersion}). There is no way to change or delete one: other methods
 * answer 405 (R-022-05).
 */
@RestController
@RequestMapping("/api/v1/groups/{groupId}/contacts/{contactId}/versions")
public class VersionController {

  private final VersionService versions;

  public VersionController(VersionService versions) {
    this.versions = versions;
  }

  @GetMapping
  @RequiresAction(Action.READ_VERSIONS)
  public List<VersionSummary> listVersions(
      @PathVariable UUID groupId, @PathVariable String contactId) {
    return versions.list(groupId, contactIdOf(contactId));
  }

  @GetMapping("/{number}")
  @RequiresAction(Action.READ_VERSIONS)
  public VersionDetail getVersion(
      @PathVariable UUID groupId, @PathVariable String contactId, @PathVariable String number) {
    return versions.get(groupId, contactIdOf(contactId), number);
  }

  @GetMapping("/{number}/compare")
  @RequiresAction(Action.READ_VERSIONS)
  public VersionComparison compareVersion(
      @PathVariable UUID groupId, @PathVariable String contactId, @PathVariable String number) {
    return versions.compare(groupId, contactIdOf(contactId), number);
  }

  /** The restore saves the content of Version {@code number} as a new Version (US-023). */
  @PostMapping("/{number}/restore")
  @RequiresAction(Action.RESTORE_VERSION)
  public Contact restoreVersion(
      @AuthenticationPrincipal AuthenticatedUser caller,
      @PathVariable UUID groupId,
      @PathVariable String contactId,
      @PathVariable String number,
      @RequestBody VersionRestoreRequest body) {
    return versions.restore(
        groupId, caller.userId(), contactIdOf(contactId), number, body.version());
  }

  /** Contract schema {@code VersionRestoreRequest}: the current version number the user saw. */
  public record VersionRestoreRequest(Integer version) {}

  private static UUID contactIdOf(String contactId) {
    try {
      return UUID.fromString(contactId);
    } catch (IllegalArgumentException e) {
      throw new ContactNotFoundException();
    }
  }
}
