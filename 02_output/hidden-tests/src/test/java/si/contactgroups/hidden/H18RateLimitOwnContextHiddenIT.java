package si.contactgroups.hidden;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.TestPropertySource;

/** Hidden cases of R-018-10 and R-018-13 with the default rate limit of 100 per minute. */
@ExtendWith(OutputCaptureExtension.class)
@TestPropertySource(properties = "RATE_LIMIT_PER_MINUTE=100")
class H18RateLimitOwnContextHiddenIT extends HiddenBase {

  @Test
  @DisplayName("R-018-10 combination two Users alternating 60 requests each are not limited; the third User's 101st is")
  void r01810() {
    U a = account("A A", "a@acme.com", true);
    U b = account("B B", "b@acme.com", true);
    for (int i = 0; i < 60; i++) {
      assertThat(a.get("/groups").status()).isEqualTo(200);
      assertThat(b.get("/groups").status()).isEqualTo(200);
    }
    U c = account("C C", "c@acme.com", true);
    for (int i = 0; i < 100; i++) {
      c.get("/me");
    }
    assertThat(c.get("/groups").status()).isEqualTo(429);
  }

  @Test
  @DisplayName("R-018-13 other a 401 with a wrong token and a 404 are logged without the token and without Contact data")
  void r01813(CapturedOutput output) {
    U ana = account("Ana Novak", "ana@acme.com", true);
    G acme = group(ana, "Acme");
    ana.post(acme.contacts(), obj("firstName", "Zaupnica")).is(201);
    U x = account("Tuja Oseba", "tuja@x.si", true);
    int before = output.getAll().length();
    send("GET", acme.contacts(), "napacen.zeton.123", null, Map.of()).is(401);
    x.get(acme.contacts()).is(404);
    String log = output.getAll().substring(before);
    assertThat(log).contains("401").contains("404");
    assertThat(log).doesNotContain("napacen.zeton.123").doesNotContain(x.token).doesNotContain("Zaupnica");
  }
}
