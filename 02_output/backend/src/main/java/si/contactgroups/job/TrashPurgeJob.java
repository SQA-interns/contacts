package si.contactgroups.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import si.contactgroups.service.GroupService;
import si.contactgroups.service.TrashService;

/**
 * Deletes the Contacts and the Groups that have been in Trash for 30 days (BR-04, ADR-09, R-013-04,
 * R-020-05).
 */
@Component
public class TrashPurgeJob {

  private static final Logger LOG = LoggerFactory.getLogger(TrashPurgeJob.class);

  private final TrashService trash;
  private final GroupService groups;

  public TrashPurgeJob(TrashService trash, GroupService groups) {
    this.trash = trash;
    this.groups = groups;
  }

  /** Runs on {@code TRASH_PURGE_CRON}; logs counts only, never data of a Contact. */
  @Scheduled(cron = "${app.trash-purge-cron}")
  public void purgeOnce() {
    int contacts = trash.purgeExpired();
    int groupCount = groups.purgeExpired();
    if (contacts > 0 || groupCount > 0) {
      LOG.info("Trash purge deleted {} Contacts and {} Groups", contacts, groupCount);
    }
  }
}
