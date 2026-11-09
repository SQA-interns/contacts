package si.contactgroups.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import si.contactgroups.domain.DuplicateFinder;
import si.contactgroups.domain.DuplicateSet;
import si.contactgroups.persistence.ContactRepository;

/** The duplicate search of a Group (US-024). It reads only and changes no data (R-024-06). */
@Service
public class DuplicateService {

  private final ContactRepository contacts;

  public DuplicateService(ContactRepository contacts) {
    this.contacts = contacts;
  }

  /** The Duplicate sets among the Contacts of the Group that are not in Trash (R-024-01). */
  @Transactional(readOnly = true)
  public List<DuplicateSet> find(UUID groupId) {
    return DuplicateFinder.find(contacts.duplicateCandidates(groupId));
  }
}
