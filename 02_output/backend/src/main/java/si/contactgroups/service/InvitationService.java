package si.contactgroups.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import si.contactgroups.domain.AcceptableInvitation;
import si.contactgroups.domain.Invitation;
import si.contactgroups.domain.Role;
import si.contactgroups.error.ApiError.FieldError;
import si.contactgroups.persistence.GroupRepository;
import si.contactgroups.persistence.InvitationRepository;
import si.contactgroups.persistence.MemberRepository;

/** Invitations to a Group (US-014): create, list, cancel and join through them. */
@Service
public class InvitationService {

  static final int MAX_PENDING = 20;
  static final Duration VALID_FOR = Duration.ofDays(14);
  private static final int MAX_EMAIL_LENGTH = 255;

  private final InvitationRepository invitations;
  private final MemberRepository members;
  private final GroupRepository groups;
  private final HistoryWriter history;
  private final Clock clock;

  public InvitationService(
      InvitationRepository invitations,
      MemberRepository members,
      GroupRepository groups,
      HistoryWriter history,
      Clock clock) {
    this.invitations = invitations;
    this.members = members;
    this.groups = groups;
    this.history = history;
    this.clock = clock;
  }

  public List<Invitation> listPending(UUID groupId) {
    return invitations.findPending(groupId, now());
  }

  /**
   * Creates a pending Invitation for the trimmed, lowercase address (R-014-01).
   *
   * @throws ValidationException when the address has no valid format (R-014-02)
   * @throws InvitationLimitException when 20 Invitations are pending (R-014-03)
   * @throws InvitationExistsException for a Member or an address already invited (R-014-04)
   */
  @Transactional
  public Invitation create(UUID groupId, UUID ownerId, String rawEmail, Role role) {
    String email = validEmail(rawEmail);
    if (role == null) {
      throw new ValidationException(List.of(new FieldError("role", "validation.required")));
    }
    Instant now = now();
    groups.lock(groupId);
    if (members.existsByEmail(groupId, email) || invitations.existsPending(groupId, email, now)) {
      throw new InvitationExistsException();
    }
    if (invitations.countPending(groupId, now) >= MAX_PENDING) {
      throw new InvitationLimitException();
    }
    Invitation created =
        invitations.insert(groupId, email, role, ownerId, now, now.plus(VALID_FOR));
    history.invitationCreated(groupId, ownerId, email, role.name(), now);
    return created;
  }

  /**
   * Marks a pending Invitation cancelled (R-014-07).
   *
   * @throws InvitationNotFoundException when the Group has no such pending Invitation
   */
  @Transactional
  public Invitation cancel(UUID groupId, UUID ownerId, UUID invitationId) {
    Instant now = now();
    Invitation cancelled =
        invitations.cancel(groupId, invitationId, now).orElseThrow(InvitationNotFoundException::new);
    history.invitationCancelled(groupId, ownerId, cancelled.email(), now);
    return cancelled;
  }

  /**
   * Makes the User a Member of every Group with a pending, unexpired Invitation for the e-mail
   * address of the User, with the invited Role (R-014-05). The caller guarantees that the address
   * is verified (R-014-06).
   */
  @Transactional
  public void acceptPendingFor(UUID userId, String email) {
    Instant now = now();
    for (AcceptableInvitation invitation : invitations.lockAcceptableFor(email, now)) {
      invitations.markAccepted(invitation.id());
      if (!members.exists(invitation.groupId(), userId)) {
        members.insert(invitation.groupId(), userId, invitation.role(), now);
        history.memberJoined(invitation.groupId(), userId, invitation.role().name(), now);
      }
    }
  }

  private Instant now() {
    return clock.instant().truncatedTo(ChronoUnit.MICROS);
  }

  private static String validEmail(String raw) {
    String email = raw == null ? "" : raw.strip().toLowerCase(Locale.ROOT);
    if (email.length() > MAX_EMAIL_LENGTH) {
      throw new ValidationException(List.of(new FieldError("email", "validation.too_long")));
    }
    if (!ContactValidator.isEmailFormat(email)) {
      throw new ValidationException(List.of(new FieldError("email", "validation.email_invalid")));
    }
    return email;
  }
}
