package si.contactgroups.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpTransport;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;

/**
 * US-001, F-26: Google serves its public signing keys gzip-compressed. The Admin SDK reads them
 * with the HTTP transport of {@code FirebaseConfig}; its default one (Apache HttpClient 5) inflated
 * the body and the Google client inflated it again ("Not in GZIP format"), so every real token was
 * refused. Emulator tokens never run this path.
 */
class FirebaseKeyFetchTest {

  private static final String BODY = "{\"kid1\":\"-----BEGIN CERTIFICATE-----\"}";

  @Test
  void theConfiguredTransportReadsAGzipResponseLikeGoogleKeyEndpoint() throws Exception {
    try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      Thread answering = new Thread(() -> answerOnceWithGzip(server));
      answering.setDaemon(true);
      answering.start();
      HttpTransport transport =
          FirebaseConfig.options("contacts-test", GoogleCredentials.newBuilder().build())
              .getHttpTransport();
      HttpRequest request =
          transport
              .createRequestFactory()
              .buildGetRequest(
                  new GenericUrl("http://127.0.0.1:" + server.getLocalPort() + "/keys"));

      assertThat(request.execute().parseAsString()).isEqualTo(BODY);
    }
  }

  private static void answerOnceWithGzip(ServerSocket server) {
    try (Socket socket = server.accept()) {
      drainRequestHead(socket.getInputStream());
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
        gzip.write(BODY.getBytes(StandardCharsets.UTF_8));
      }
      OutputStream out = socket.getOutputStream();
      out.write(
          ("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Encoding: gzip\r\n"
                  + "Content-Length: "
                  + bytes.size()
                  + "\r\nConnection: close\r\n\r\n")
              .getBytes(StandardCharsets.US_ASCII));
      bytes.writeTo(out);
      out.flush();
    } catch (java.io.IOException e) {
      throw new java.io.UncheckedIOException(e);
    }
  }

  private static void drainRequestHead(InputStream in) throws java.io.IOException {
    int matched = 0;
    int b;
    while (matched < 4 && (b = in.read()) != -1) {
      boolean expected = b == (matched % 2 == 0 ? '\r' : '\n');
      matched = expected ? matched + 1 : (b == '\r' ? 1 : 0);
    }
  }
}
