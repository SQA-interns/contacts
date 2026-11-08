package si.contactgroups.acceptance.support;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/** HTTP calls to the running backend (the public REST interface, base path {@code /api/v1}). */
public final class Http {

  public static final String API = "/api/v1";
  private static final HttpClient CLIENT =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  private final int port;

  public Http(int port) {
    this.port = port;
  }

  /**
   * Sends a request. {@code path} starting with {@code /api/} or {@code /health} is used as is,
   * any other path is put under {@code /api/v1}. {@code body} is a JSON value, a raw {@code
   * String} (sent as is) or a {@code byte[]}; {@code token} may be {@code null}.
   */
  public Resp send(
      String method, String path, String token, Object body, Map<String, String> headers) {
    String full = path.startsWith("/api/") || path.startsWith("/health") ? path : API + path;
    HttpRequest.Builder request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + full))
            .timeout(Duration.ofSeconds(120));
    if (token != null) {
      request.header("Authorization", "Bearer " + token);
    }
    boolean hasContentType =
        headers.keySet().stream().anyMatch(name -> name.equalsIgnoreCase("Content-Type"));
    headers.forEach(request::header);
    if (body == null) {
      request.method(method, HttpRequest.BodyPublishers.noBody());
    } else if (body instanceof byte[] raw) {
      if (!hasContentType) {
        request.header("Content-Type", "application/json");
      }
      request.method(method, HttpRequest.BodyPublishers.ofByteArray(raw));
    } else {
      if (!hasContentType) {
        request.header("Content-Type", "application/json");
      }
      String text = body instanceof String s ? s : Json.write(body);
      request.method(method, HttpRequest.BodyPublishers.ofString(text));
    }
    try {
      HttpResponse<byte[]> response =
          CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
      return new Resp(response.statusCode(), response.body(), response.headers());
    } catch (java.io.IOException e) {
      throw new java.io.UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  public Resp send(String method, String path, String token, Object body) {
    return send(method, path, token, body, Map.of());
  }
}
