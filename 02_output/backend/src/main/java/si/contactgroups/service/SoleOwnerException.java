package si.contactgroups.service;

import java.util.List;
import si.contactgroups.domain.SoleOwnerGroup;

/** The User is the only Owner of at least one Group and cannot delete the account (R-021-02). */
public class SoleOwnerException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final transient List<SoleOwnerGroup> soleOwned;

  public SoleOwnerException(List<SoleOwnerGroup> groups) {
    super("The user is the only owner of a group");
    this.soleOwned = List.copyOf(groups);
  }

  /** The Groups to hand over or delete first. */
  public List<SoleOwnerGroup> groups() {
    return soleOwned;
  }
}
