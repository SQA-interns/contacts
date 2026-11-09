package si.contactgroups.domain;

/** The stored state of an Invitation; an unaccepted one past its expiry counts as expired. */
public enum InvitationStatus {
  PENDING,
  ACCEPTED,
  CANCELLED,
  EXPIRED
}
