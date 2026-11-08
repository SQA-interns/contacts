package si.contactgroups.acceptance.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** One HTTP answer of the REST interface. */
public record Resp(int status, byte[] bytes, HttpHeaders headers) {

  public String body() {
    return new String(bytes, StandardCharsets.UTF_8);
  }

  public JsonNode json() {
    return Json.parse(body());
  }

  /** The error code of the error format (ADR-10), or an empty string. */
  public String code() {
    JsonNode json = json();
    return json != null && json.has("code") ? json.get("code").asText() : "";
  }

  /** The {@code field} values of the {@code fieldErrors} of a 422 answer. */
  public List<String> errorFields() {
    return Json.field(json().path("fieldErrors"), "field");
  }

  public List<String> errorCodes() {
    return Json.field(json().path("fieldErrors"), "code");
  }

  public String header(String name) {
    return headers.firstValue(name).orElse(null);
  }

  /** Asserts the status and returns this answer. */
  public Resp expect(int expected) {
    assertThat(status).as("HTTP status; body: %s", abbreviate(body())).isEqualTo(expected);
    return this;
  }

  /** Asserts the status and the error code and returns this answer. */
  public Resp expectError(int expected, String errorCode) {
    expect(expected);
    assertThat(code()).as("error code; body: %s", abbreviate(body())).isEqualTo(errorCode);
    return this;
  }

  public String id() {
    return json().path("id").asText();
  }

  public int version() {
    return json().path("version").asInt();
  }

  private static String abbreviate(String text) {
    return text.length() > 600 ? text.substring(0, 600) + "…" : text;
  }
}
