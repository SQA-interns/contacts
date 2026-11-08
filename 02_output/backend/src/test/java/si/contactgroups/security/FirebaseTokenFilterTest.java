package si.contactgroups.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import si.contactgroups.domain.User;
import si.contactgroups.service.EmailTakenException;
import si.contactgroups.service.UserService;

/** US-001 and ADR-04: the filter checks the token and resolves the User of the request. */
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
  private final FirebaseTokenFilter filter = new FirebaseTokenFilter(verifier, users);

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
    when(users.findOrCreate(ANA))
        .thenReturn(new User(id, "uid-ana", "ana@acme.com", "Ana Novak", Instant.EPOCH));
    MockFilterChain chain = new MockFilterChain();

    call("valid", chain);

    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    assertThat(principal).isEqualTo(new AuthenticatedUser(id, "ana@acme.com", false));
    assertThat(chain.getRequest()).isNotNull();
  }

  @Test
  void d_18d_a_token_whose_email_belongs_to_another_user_is_409() throws Exception {
    when(users.findOrCreate(ANA)).thenThrow(new EmailTakenException());
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
}
