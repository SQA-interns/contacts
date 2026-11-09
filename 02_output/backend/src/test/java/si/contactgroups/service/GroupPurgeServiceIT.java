package si.contactgroups.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import si.contactgroups.domain.User;

/** US-020: the clean-up deletes a Group after 30 days (R-020-05, BR-04). */
class GroupPurgeServiceIT extends GroupTrashTestSupport {

  @Test
  void r_020_05_the_clean_up_deletes_a_group_after_30_days_with_everything_in_it() {
    User ana = newUser();
    UUID acme = newGroup(ana, "Acme");
    jdbc.sql(
            "INSERT INTO invitation (group_id, email, role, status, created_at, expires_at, created_by)"
                + " VALUES (:g, 'x@acme.com', 'VIEWER', 'PENDING', now(), now() + interval '14 days',"
                + " :u)")
        .param("g", acme)
        .param("u", ana.id())
        .update();
    groups.trashGroup(acme, "Acme");
    trashedAgo(acme, Duration.ofDays(30).plusMinutes(1));

    int deleted = groups.purgeExpired();

    assertThat(deleted).isGreaterThanOrEqualTo(1);
    assertThat(count("contact_group", acme)).isZero();
    assertThat(count("member", acme)).isZero();
    assertThat(count("invitation", acme)).isZero();
  }

  @Test
  void r_020_05_the_clean_up_keeps_a_group_that_is_younger_than_30_days() {
    User ana = newUser();
    UUID acme = newGroup(ana, "Acme");
    groups.trashGroup(acme, "Acme");
    trashedAgo(acme, Duration.ofDays(29).plusHours(23));

    groups.purgeExpired();

    assertThat(count("contact_group", acme)).isEqualTo(1);
  }
}
