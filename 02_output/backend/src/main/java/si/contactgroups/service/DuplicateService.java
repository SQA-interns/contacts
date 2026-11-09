package si.contactgroups.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import si.contactgroups.domain.DismissedSets;
import si.contactgroups.domain.DuplicateFinder;
import si.contactgroups.domain.DuplicateSet;
import si.contactgroups.persistence.ContactRepository;
import si.contactgroups.persistence.DismissalRepository;

/**
 * The duplicate search of a Group (US-024), without the sets marked "not a duplicate" (US-025). The
 * search changes no data (R-024-06); dismissing stores only the dismissal and no Contact changes
 * (R-025-05).
 */
@Service
public class DuplicateService {

  private final ContactRepository contacts;
  private final DismissalRepository dismissals;

  public DuplicateService(ContactRepository contacts, DismissalRepository dismissals) {
    this.contacts = contacts;
    this.dismissals = dismissals;
  }

  /** The Duplicate sets among the Contacts of the Group not in Trash that nobody dismissed. */
  @Transactional(readOnly = true)
  public List<DuplicateSet> find(UUID groupId) {
    return DismissedSets.hide(allSets(groupId), dismissals.dismissedSets(groupId));
  }

  /**
   * Marks the set of exactly these Contacts "not a duplicate" (R-025-01). A set that is already
   * dismissed is stored once, so two Editors dismissing the same set both succeed (EX-025-04).
   *
   * @throws DuplicateSetNotFoundException when the Contacts are no set of the Group (EX-025-03)
   */
  @Transactional
  public void dismiss(UUID groupId, UUID userId, List<UUID> contactIds) {
    Set<UUID> ids = contactIds == null ? Set.of() : new HashSet<>(contactIds);
    String key = DuplicateFinder.keyOf(ids);
    boolean isSet = allSets(groupId).stream().anyMatch(set -> set.key().equals(key));
    if (!isSet) {
      throw new DuplicateSetNotFoundException();
    }
    dismissals.add(groupId, key, userId, ids);
  }

  private List<DuplicateSet> allSets(UUID groupId) {
    return DuplicateFinder.find(contacts.duplicateCandidates(groupId));
  }
}
