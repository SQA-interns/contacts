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
import si.contactgroups.domain.TagCount;
import si.contactgroups.security.AuthenticatedUser;
import si.contactgroups.security.RequiresAction;
import si.contactgroups.service.TagNotFoundException;
import si.contactgroups.service.TagService;

/** The Tags of a Group (contract {@code listTags}, {@code renameTag}, {@code deleteTag}). */
@RestController
@RequestMapping("/api/v1/groups/{groupId}/tags")
public class TagController {

  private final TagService tags;

  public TagController(TagService tags) {
    this.tags = tags;
  }

  @GetMapping
  @RequiresAction(Action.MANAGE_TAGS)
  public List<TagCount> listTags(@PathVariable UUID groupId) {
    return tags.listWithCounts(groupId);
  }

  @PatchMapping("/{tagId}")
  @RequiresAction(Action.MANAGE_TAGS)
  public TagCount renameTag(
      @AuthenticationPrincipal AuthenticatedUser caller,
      @PathVariable UUID groupId,
      @PathVariable String tagId,
      @RequestBody TagRename body) {
    return tags.rename(groupId, caller.userId(), tagIdOf(tagId), body.name());
  }

  @DeleteMapping("/{tagId}")
  @RequiresAction(Action.MANAGE_TAGS)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteTag(
      @AuthenticationPrincipal AuthenticatedUser caller,
      @PathVariable UUID groupId,
      @PathVariable String tagId) {
    tags.delete(groupId, caller.userId(), tagIdOf(tagId));
  }

  private static UUID tagIdOf(String tagId) {
    try {
      return UUID.fromString(tagId);
    } catch (IllegalArgumentException e) {
      throw new TagNotFoundException();
    }
  }

  /** Contract schema {@code TagRename}. */
  public record TagRename(String name) {}
}
