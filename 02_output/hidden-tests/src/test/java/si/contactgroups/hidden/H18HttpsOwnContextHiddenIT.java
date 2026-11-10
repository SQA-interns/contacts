package si.contactgroups.hidden;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/** Hidden case of R-018-12 with REQUIRE_HTTPS=true. */
@TestPropertySource(properties = "REQUIRE_HTTPS=true")
class H18HttpsOwnContextHiddenIT extends HiddenBase {

  @Test
  @DisplayName("R-018-12 invalid a request forwarded as http is refused even without a token")
  void r01812() {
    Resp r = send("GET", "/api/v1/groups", null, null, Map.of("X-Forwarded-Proto", "http"));
    assertThat(r.status()).isEqualTo(403);
    assertThat(r.code()).isEqualTo("https.required");
  }
}
