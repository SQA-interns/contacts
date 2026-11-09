package si.contactgroups.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

/** D-69 (review F-14): the Admin SDK needs a service account to read an account in production. */
class FirebaseCredentialsTest {

  @TempDir Path dir;

  @Test
  void withoutTheVariableTheCredentialsAreEmpty() {
    GoogleCredentials credentials = FirebaseCredentials.load(new MockEnvironment());

    assertThat(credentials).isNotInstanceOf(ServiceAccountCredentials.class);
  }

  @Test
  void aBlankVariableIsTheSameAsUnset() {
    MockEnvironment env = new MockEnvironment().withProperty("GOOGLE_APPLICATION_CREDENTIALS", " ");

    assertThat(FirebaseCredentials.load(env)).isNotInstanceOf(ServiceAccountCredentials.class);
  }

  @Test
  void theServiceAccountFileIsRead() throws Exception {
    Path file = ServiceAccountFiles.create(dir);
    MockEnvironment env =
        new MockEnvironment().withProperty("GOOGLE_APPLICATION_CREDENTIALS", file.toString());

    GoogleCredentials credentials = FirebaseCredentials.load(env);

    assertThat(credentials).isInstanceOf(ServiceAccountCredentials.class);
    assertThat(((ServiceAccountCredentials) credentials).getClientEmail())
        .isEqualTo("admin@demo-contact-groups.iam.gserviceaccount.com");
  }

  @Test
  void aMissingFileIsRefusedWithoutNamingItsContent() {
    String missing = dir.resolve("absent.json").toString();
    MockEnvironment env =
        new MockEnvironment().withProperty("GOOGLE_APPLICATION_CREDENTIALS", missing);

    assertThatThrownBy(() -> FirebaseCredentials.load(env))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("GOOGLE_APPLICATION_CREDENTIALS");
  }

  @Test
  void aFileThatIsNoServiceAccountIsRefusedWithoutEchoingIt() throws Exception {
    Path file = dir.resolve("bad.json");
    Files.writeString(file, "{\"private_key\": \"do-not-print-this\"}");
    MockEnvironment env =
        new MockEnvironment().withProperty("GOOGLE_APPLICATION_CREDENTIALS", file.toString());

    assertThatThrownBy(() -> FirebaseCredentials.load(env))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("GOOGLE_APPLICATION_CREDENTIALS")
        .hasMessageNotContaining("do-not-print-this");
  }
}
