package si.contactgroups.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import si.contactgroups.domain.HistoryAction;
import si.contactgroups.domain.TagChanges;
import si.contactgroups.domain.TagCount;
import si.contactgroups.error.ApiError.FieldError;
import si.contactgroups.persistence.TagRepository;

/** Puts Tags on and takes them off a Contact as part of its save and records it (US-005). */
@Service
public class TagService {

  private final TagRepository tags;
  private final HistoryWriter history;
  private final Clock clock;

  public TagService(TagRepository tags, HistoryWriter history, Clock clock) {
    this.tags = tags;
    this.history = history;
    this.clock = clock;
  }

  /** The Tags of the Group with their Contact counts (R-006-01). */
  public List<TagCount> listWithCounts(UUID groupId) {
    return tags.counts(groupId);
  }

  /**
   * Renames the Tag on all Contacts (R-006-02..04) with one History entry per Contact and no
   * version change (R-006-07).
   */
  @Transactional
  public TagCount rename(UUID groupId, UUID userId, UUID tagId, String newName) {
    List<FieldError> errors = new ArrayList<>();
    String name = TagCheck.cleanName(newName, errors);
    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
    TagCount tag = tags.find(groupId, tagId).orElseThrow(TagNotFoundException::new);
    if (tags.nameTakenByOther(groupId, tagId, name)) {
      throw new TagNameTakenException();
    }
    if (tag.name().equals(name)) {
      return tag;
    }
    tags.rename(groupId, tagId, name);
    Instant now = clock.instant();
    for (UUID contactId : tags.contactIds(groupId, tagId)) {
      history.tagRenamed(groupId, userId, contactId, tag.name(), name, now);
    }
    return new TagCount(tagId, name, tag.contactCount());
  }

  /** Removes the Tag from all Contacts, Trash included, and deletes it (R-006-05, R-006-07). */
  @Transactional
  public void delete(UUID groupId, UUID userId, UUID tagId) {
    TagCount tag = tags.find(groupId, tagId).orElseThrow(TagNotFoundException::new);
    Instant now = clock.instant();
    for (UUID contactId : tags.contactIds(groupId, tagId)) {
      history.tagDeleted(groupId, userId, contactId, tag.name(), now);
    }
    tags.delete(groupId, tagId);
  }

  public List<String> list(UUID groupId, UUID contactId) {
    return tags.names(groupId, contactId);
  }

  /** Trims the typed names and adds every broken rule (R-005-02, R-005-04) to {@code errors}. */
  List<String> clean(List<String> sent, List<FieldError> errors) {
    return TagCheck.clean(sent, errors);
  }

  /**
   * Makes the Tags of the Contact equal to {@code after} and writes one History entry per added or
   * removed Tag (R-005-08).
   *
   * @return whether anything changed
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public boolean replace(
      UUID groupId,
      UUID userId,
      UUID contactId,
      List<String> before,
      List<String> after,
      Instant at) {
    TagChanges changes = TagChanges.between(before, after);
    for (String typed : changes.added()) {
      String name = tags.attach(groupId, contactId, typed);
      history.tagChanged(groupId, userId, contactId, HistoryAction.TAG_ADDED, name, at);
    }
    for (String name : changes.removed()) {
      tags.detach(groupId, contactId, name);
      history.tagChanged(groupId, userId, contactId, HistoryAction.TAG_REMOVED, name, at);
    }
    return !changes.isEmpty();
  }
}
