package si.contactgroups.service;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import si.contactgroups.domain.Role;
import si.contactgroups.persistence.MemberRepository;

/** Who is a Member of which Group, with which Role (glossary: Member). */
@Service
public class MembershipService {

  private final MemberRepository members;

  public MembershipService(MemberRepository members) {
    this.members = members;
  }

  /** Makes the User the Owner of a Group that has just been created (R-002-01). */
  void addCreatorAsOwner(UUID groupId, UUID userId, Instant joinedAt) {
    members.insert(groupId, userId, Role.OWNER, joinedAt);
  }
}
