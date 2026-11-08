package si.contactgroups.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import si.contactgroups.acceptance.support.AcceptanceTestBase;
import si.contactgroups.acceptance.support.Resp;
import si.contactgroups.acceptance.support.TestUser;

/**
 * US-018 HTTPS only (R-018-12) with {@code REQUIRE_HTTPS=true} as in production, where TLS ends at
 * the reverse proxy and the proxy forwards {@code X-Forwarded-Proto: https} (environments.md).
 */
@TestPropertySource(properties = "REQUIRE_HTTPS=true")
class Us018HttpsOwnContextAcceptanceIT extends AcceptanceTestBase {

  @Test
  @DisplayName("AC-018-14 a plain HTTP request is refused; the same request over HTTPS is answered")
  void ac_018_14_plain_http_refused() {
    TestUser ana = account("Ana Novak", "ana@acme.com", true);

    Resp plain = ana.get("/groups");
    Resp secure = ana.send("GET", "/groups", null, Map.of("X-Forwarded-Proto", "https"));

    assertThat(plain.status()).isEqualTo(403);
    assertThat(plain.code()).isEqualTo("https.required");
    assertThat(secure.status()).isEqualTo(200);
  }
}
