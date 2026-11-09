package si.contactgroups.domain;

import java.time.Instant;
import java.util.UUID;

/** Contract schema {@code TrashContact}. */
public record TrashContactView(
    UUID id,
    String firstName,
    String lastName,
    String email,
    String company,
    Instant deletedAt,
    String deletedBy,
    int daysLeft) {

  public static TrashContactView of(TrashedContact c, Instant now) {
    return new TrashContactView(
        c.id(),
        c.firstName(),
        c.lastName(),
        c.email(),
        c.company(),
        c.deletedAt(),
        c.deletedBy(),
        TrashPeriod.daysLeft(c.deletedAt(), now));
  }
}
