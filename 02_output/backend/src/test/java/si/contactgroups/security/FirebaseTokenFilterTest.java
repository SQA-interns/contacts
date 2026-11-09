package si.contactgroups.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Predicate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import si.contactgroups.domain.User;
import si.contactgroups.service.EmailTakenException;
import si.contactgroups.service.UserService;

/** US-001 and ADR-04: the filter checks the token and resolves the User of the request. */
@ExtendWith(OutputCaptureExtension.class)
class FirebaseTokenFilterTest {

  private static final VerifiedToken ANA =
      new VerifiedToken("uid-ana", "Ana@Acme.COM", false, "Ana Novak");

  private final UserService users = mock(UserService.class);
  private final TokenVerifier verifier =
      token -> {
        if (token.equals("valid")) {
          return ANA;
        }
        throw new InvalidTokenException("rejected", null);
      };
  private final AccountDirectory directory = mock(AccountDirectory.class);
  private final FirebaseTokenFilter filter = new FirebaseTokenFilter(verifier, users, directory);

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  private MockHttpServletResponse call(String token, MockFilterChain chain) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/me");
    request.addHeader("Authorization", "Bearer " + token);
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, chain);
    return response;
  }

  @Test
  void r_001_11_a_valid_token_gives_the_user_id_and_the_verified_flag() throws Exception {
    UUID id = UUID.randomUUID();
    when(users.findOrCreate(eq(ANA), any()))
        .thenReturn(new User(id, "uid-ana", "ana@acme.com", "Ana Novak", Instant.EPOCH));
    MockFilterChain chain = new MockFilterChain();

    call("valid", chain);

    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    assertThat(principal).isEqualTo(new AuthenticatedUser(id, "ana@acme.com", false));
    assertThat(chain.getRequest()).isNotNull();
  }

  @Test
  void d_18d_a_token_whose_email_belongs_to_another_user_is_409() throws Exception {
    when(users.findOrCreate(eq(ANA), any())).thenThrow(new EmailTakenException());
    MockFilterChain chain = new MockFilterChain();

    MockHttpServletResponse response = call("valid", chain);

    assertThat(response.getStatus()).isEqualTo(409);
    assertThat(response.getContentAsString()).contains("\"code\":\"user.email_taken\"");
    assertThat(chain.getRequest()).isNull();
  }

  @Test
  void r_001_12_an_invalid_or_expired_token_is_401() throws Exception {
    MockFilterChain chain = new MockFilterChain();

    MockHttpServletResponse response = call("expired", chain);

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentAsString()).contains("\"code\":\"auth.token_invalid\"");
    assertThat(chain.getRequest()).isNull();
  }

  @Test
  void r_021_05_a_token_of_a_deleted_account_is_401_and_the_user_is_not_created() throws Exception {
    when(users.findOrCreate(eq(ANA), any())).thenThrow(new InvalidTokenException("gone", null));
    MockFilterChain chain = new MockFilterChain();

    MockHttpServletResponse response = call("valid", chain);

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentAsString()).contains("\"code\":\"auth.token_invalid\"");
    assertThat(chain.getRequest()).isNull();
  }

  @Test
  void r_021_05_the_filter_asks_firebase_whether_the_account_exists() throws Exception {
    when(directory.exists("uid-ana")).thenReturn(false);
    when(users.findOrCreate(eq(ANA), any()))
        .thenReturn(new User(UUID.randomUUID(), "uid-ana", "ana@acme.com", "Ana", Instant.EPOCH));

    call("valid", new MockFilterChain());

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Predicate<String>> exists = ArgumentCaptor.forClass(Predicate.class);
    verify(users).findOrCreate(eq(ANA), exists.capture());
    assertThat(exists.getValue().test("uid-ana")).isFalse();
    verify(directory).exists("uid-ana");
  }

  @Test
  void f_26_a_rejected_token_logs_the_class_of_the_cause_and_nothing_of_the_token(
      CapturedOutput output) throws Exception {
    TokenVerifier failing =
        token -> {
          throw new InvalidTokenException(
              "token rejected",
              new IllegalStateException("keys", new java.util.zip.ZipException("secret-detail")));
        };
    FirebaseTokenFilter rejecting = new FirebaseTokenFilter(failing, users, directory);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/me");
    request.addHeader("Authorization", "Bearer eyJ.secret.token");

    rejecting.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertThat(output.getAll())
        .contains("IllegalStateException")
        .contains("ZipException")
        .doesNotContain("eyJ.secret.token")
        .doesNotContain("secret-detail");
  }
}
