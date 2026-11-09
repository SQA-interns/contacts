package si.contactgroups.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import si.contactgroups.domain.AttributeChanges;
import si.contactgroups.domain.AttributePair;
import si.contactgroups.error.ApiError.FieldError;
import si.contactgroups.persistence.AttributeRepository;

/** Stores the Attributes of a Contact as part of its save and records each change (US-004). */
@Service
public class AttributeService {

  private final AttributeRepository attributes;
  private final HistoryWriter history;

  public AttributeService(AttributeRepository attributes, HistoryWriter history) {
    this.attributes = attributes;
    this.history = history;
  }

  public List<AttributePair> list(UUID groupId, UUID contactId) {
    return attributes.list(groupId, contactId);
  }

  /** Trims the sent pairs and adds every broken rule (R-004-02..04) to {@code errors}. */
  List<AttributePair> clean(List<AttributePair> sent, List<FieldError> errors) {
    return AttributeCheck.clean(sent, errors);
  }

  /**
   * Makes the stored pairs equal to {@code after} and writes one History entry per added, changed
   * or removed pair (R-004-06).
   *
   * @return whether anything changed
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public boolean replace(
      UUID groupId,
      UUID userId,
      UUID contactId,
      List<AttributePair> before,
      List<AttributePair> after,
      Instant at) {
    List<AttributeChanges.Change> changes = AttributeChanges.between(before, after);
    for (AttributeChanges.Change change : changes) {
      switch (change.action()) {
        case ATTRIBUTE_ADDED ->
            attributes.insert(
                groupId, contactId, new AttributePair(change.key(), change.newValue()));
        case ATTRIBUTE_CHANGED ->
            attributes.updateValue(groupId, contactId, change.key(), change.newValue());
        default -> attributes.delete(groupId, contactId, change.key());
      }
      history.attributeChanged(groupId, userId, contactId, change, at);
    }
    return !changes.isEmpty();
  }
}
