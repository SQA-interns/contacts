package si.contactgroups.domain;

import java.time.Instant;
import java.util.UUID;

/** Contract schema {@code GroupTrashItem}: a Group in the Group Trash with the days left. */
public record GroupTrashItem(UUID id, String name, Instant deletedAt, int daysLeft) {

  public static GroupTrashItem of(UUID id, String name, Instant deletedAt, Instant now) {
    return new GroupTrashItem(id, name, deletedAt, TrashPeriod.daysLeft(deletedAt, now));
  }
}
