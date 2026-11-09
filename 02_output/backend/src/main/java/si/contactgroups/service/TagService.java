package si.contactgroups.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import si.contactgroups.domain.HistoryAction;
import si.contactgroups.domain.TagChanges;
import si.contactgroups.error.ApiError.FieldError;
import si.contactgroups.persistence.TagRepository;

/** Puts Tags on and takes them off a Contact as part of its save and records it (US-005). */
@Service
public class TagService {

  private final TagRepository tags;
  private final HistoryWriter history;

  public TagService(TagRepository tags, HistoryWriter history) {
    this.tags = tags;
    this.history = history;
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
