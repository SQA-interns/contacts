package si.contactgroups.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import si.contactgroups.domain.User;
import si.contactgroups.security.VerifiedToken;
import si.contactgroups.support.ApplicationContextSupport;

/** US-002: the Owner name rule holds for requests of the same User at the same time. */
class GroupServiceIT extends ApplicationContextSupport {

  private static final Duration WAIT = Duration.ofSeconds(2);

  @Autowired private GroupService groups;
  @Autowired private UserService users;
  @Autowired private JdbcClient jdbc;
  @Autowired private PlatformTransactionManager transactions;

  private User newUser() {
    String uid = "uid-" + UUID.randomUUID();
    return users.findOrCreate(new VerifiedToken(uid, uid + "@acme.com", true, "Ana Novak"));
  }

  @Test
  void r_002_03_a_create_waits_for_a_create_of_the_same_owner_in_progress() throws Exception {
    User ana = newUser();
    TransactionStatus first = transactions.getTransaction(new DefaultTransactionDefinition());
    groups.createGroup(ana.id(), "Acme");

    CompletableFuture<?> second =
        CompletableFuture.runAsync(() -> groups.createGroup(ana.id(), "acme"));
    boolean secondWaited = waitsLongerThan(second, WAIT);
    transactions.commit(first);

    assertThat(secondWaited).as("the second create waits for the first").isTrue();
    assertThatThrownBy(() -> second.get(WAIT.toMillis(), TimeUnit.MILLISECONDS))
        .isInstanceOf(ExecutionException.class)
        .hasCauseInstanceOf(GroupNameTakenException.class);
    assertThat(
            jdbc.sql("SELECT count(*) FROM member WHERE user_id = :u")
                .param("u", ana.id())
                .query(Integer.class)
                .single())
        .isEqualTo(1);
  }

  private static boolean waitsLongerThan(CompletableFuture<?> future, Duration wait)
      throws InterruptedException {
    try {
      future.get(wait.toMillis(), TimeUnit.MILLISECONDS);
      return false;
    } catch (TimeoutException e) {
      return true;
    } catch (ExecutionException e) {
      return false;
    }
  }
}
