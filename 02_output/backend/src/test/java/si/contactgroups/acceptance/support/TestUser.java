package si.contactgroups.acceptance.support;

import java.util.Map;

/**
 * A person with an account in the Firebase emulator who calls the REST interface with their own ID
 * token. The password is a test value that exists only in the emulator.
 */
public final class TestUser {

  public static final String PASSWORD = "Geslo1234";

  private final Http http;
  private final String email;
  private final String displayName;
  private final String uid;
  private String token;

  public TestUser(Http http, String email, String displayName, String uid, String token) {
    this.http = http;
    this.email = email;
    this.displayName = displayName;
    this.uid = uid;
    this.token = token;
  }

  public String email() {
    return email;
  }

  public String displayName() {
    return displayName;
  }

  public String uid() {
    return uid;
  }

  public String token() {
    return token;
  }

  /** Signs in again, so that the token carries the current claims (e.g. a verified e-mail). */
  public TestUser refresh() {
    token = Emulator.signIn(email, PASSWORD);
    return this;
  }

  public Resp get(String path) {
    return http.send("GET", path, token, null);
  }

  public Resp post(String path, Object body) {
    return http.send("POST", path, token, body);
  }

  public Resp post(String path) {
    return http.send("POST", path, token, null);
  }

  public Resp put(String path, Object body) {
    return http.send("PUT", path, token, body);
  }

  public Resp patch(String path, Object body) {
    return http.send("PATCH", path, token, body);
  }

  public Resp delete(String path) {
    return http.send("DELETE", path, token, null);
  }

  public Resp send(String method, String path, Object body, Map<String, String> headers) {
    return http.send(method, path, token, body, headers);
  }

  /** The id of this User in the product ({@code GET /me}). */
  public String userId() {
    return get("/me").expect(200).json().path("id").asText();
  }

  @Override
  public String toString() {
    return displayName + " <" + email + ">";
  }
}
