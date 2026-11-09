package si.contactgroups.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import si.contactgroups.service.TrashService;

/** Deletes the Contacts that have been in Trash for 30 days (BR-04, ADR-09, R-013-04). */
@Component
public class TrashPurgeJob {

  private static final Logger LOG = LoggerFactory.getLogger(TrashPurgeJob.class);

  private final TrashService trash;

  public TrashPurgeJob(TrashService trash) {
    this.trash = trash;
  }

  /** Runs on {@code TRASH_PURGE_CRON}; logs counts only, never data of a Contact. */
  @Scheduled(cron = "${app.trash-purge-cron}")
  public void purgeOnce() {
    int deleted = trash.purgeExpired();
    if (deleted > 0) {
      LOG.info("Trash purge deleted {} Contacts", deleted);
    }
  }
}
