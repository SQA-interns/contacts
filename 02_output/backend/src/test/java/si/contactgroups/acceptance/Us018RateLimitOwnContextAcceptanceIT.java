package si.contactgroups.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.TestPropertySource;
import si.contactgroups.acceptance.support.AcceptanceTestBase;
import si.contactgroups.acceptance.support.Grp;
import si.contactgroups.acceptance.support.Resp;
import si.contactgroups.acceptance.support.TestUser;

/**
 * US-018 rate limit (R-018-10) and security log (R-018-13), with the default {@code
 * RATE_LIMIT_PER_MINUTE} of 100 (environments.md). The users of these tests are created with {@link
 * #account}, which sends no request to the product, so that every request is counted here.
 */
@ExtendWith(OutputCaptureExtension.class)
@TestPropertySource(properties = "RATE_LIMIT_PER_MINUTE=100")
class Us018RateLimitOwnContextAcceptanceIT extends AcceptanceTestBase {

  private static List<Integer> send(TestUser user, int count) {
    return java.util.stream.IntStream.range(0, count)
        .mapToObj(i -> user.get("/groups").status())
        .toList();
  }

  @Test
  @DisplayName(
      "AC-018-12 the 101st request in a minute answers 429; after the minute requests work again")
  void ac_018_12_limit_and_recovery() {
    TestUser ana = account("Ana Novak", "ana@acme.com", true);
    assertThat(send(ana, 100)).allMatch(status -> status == 200);

    Resp limited = ana.get("/groups");

    assertThat(limited.status()).isEqualTo(429);
    assertThat(limited.code()).isEqualTo("rate.limited");
    assertThat(limited.json().path("message").asText()).isNotBlank();

    clock.advance(Duration.ofSeconds(61));
    Instant giveUp = Instant.now().plusSeconds(75);
    int status = ana.get("/groups").status();
    while (status == 429 && Instant.now().isBefore(giveUp)) {
      sleep(5000);
      status = ana.get("/groups").status();
    }
    assertThat(status).isEqualTo(200);
  }

  @Test
  @DisplayName("EX-018-03 exactly 100 requests in a minute are all accepted")
  void ex_018_03_exactly_100_accepted() {
    TestUser ana = account("Ana Novak", "ana@acme.com", true);

    assertThat(send(ana, 100)).allMatch(status -> status == 200);
  }

  @Test
  @DisplayName("EX-018-09 two Users sending 100 requests each are not limited")
  void ex_018_09_limit_is_per_user() {
    TestUser ana = account("Ana Novak", "ana@acme.com", true);
    TestUser boris = account("Boris Kos", "boris@acme.com", true);

    List<Integer> anas = send(ana, 100);
    List<Integer> boriss = send(boris, 100);

    assertThat(anas).allMatch(status -> status == 200);
    assertThat(boriss).allMatch(status -> status == 200);
  }

  @Test
  @DisplayName(
      "AC-018-15 failed requests (401, 404, 403, 429) are logged one line each, without Contact data or token")
  void ac_018_15_security_log(CapturedOutput output) {
    TestUser ana = account("Ana Novak", "ana@acme.com", true);
    Grp acme = group(ana, "Acme");
    ana.post(acme.contacts(), contactBody("firstName", "Skrivnost", "lastName", "Zaupna"))
        .expect(201);
    ana.post(
            acme.invitations(),
            si.contactgroups.acceptance.support.Json.obj(
                "email", "viola@acme.com", "role", "VIEWER"))
        .expect(201);
    TestUser viola = account("Viola Vidmar", "viola@acme.com", true);
    viola.get("/groups").expect(200);
    TestUser boris = account("Boris Kos", "boris@acme.com", true);
    int before = output.getAll().length();

    anonymous("GET", acme.contacts(), null).expect(401);
    boris.get(acme.contacts()).expect(404);
    viola
        .post(acme.contacts(), contactBody("firstName", "Skrivnost", "lastName", "Zaupna"))
        .expect(403);
    send(boris, 99);
    boris.get("/groups").expect(429);

    String log = output.getAll().substring(before);
    List<String> lines = log.lines().toList();
    assertThat(lines).anyMatch(l -> l.contains("401"));
    assertThat(lines).anyMatch(l -> l.contains("404"));
    assertThat(lines).anyMatch(l -> l.contains("403"));
    assertThat(lines).anyMatch(l -> l.contains("429"));
    assertThat(log)
        .doesNotContain("Skrivnost")
        .doesNotContain("Zaupna")
        .doesNotContain(ana.token())
        .doesNotContain(boris.token())
        .doesNotContain(viola.token());
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
