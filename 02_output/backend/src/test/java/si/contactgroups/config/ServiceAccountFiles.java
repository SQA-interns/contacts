package si.contactgroups.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.util.Base64;

/**
 * A service-account key file for tests, made with a throw-away key so none is in the repository.
 */
final class ServiceAccountFiles {

  private ServiceAccountFiles() {}

  static Path create(Path dir) throws IOException, GeneralSecurityException {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    String der =
        Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
            .encodeToString(generator.generateKeyPair().getPrivate().getEncoded());
    String pem = "-----BEGIN PRIVATE KEY-----\n" + der + "\n-----END PRIVATE KEY-----\n";
    String json =
        "{\"type\":\"service_account\",\"project_id\":\"demo-contact-groups\","
            + "\"private_key_id\":\"k1\",\"private_key\":\""
            + pem.replace("\n", "\\n")
            + "\",\"client_email\":\"admin@demo-contact-groups.iam.gserviceaccount.com\","
            + "\"client_id\":\"1\",\"token_uri\":\"https://oauth2.googleapis.com/token\"}";
    Path file = dir.resolve("service-account.json");
    Files.writeString(file, json);
    return file;
  }
}
