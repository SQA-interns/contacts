package si.contactgroups.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import si.contactgroups.domain.User;
import si.contactgroups.service.EmailTakenException;
import si.contactgroups.service.UserService;

/**
 * Checks the Firebase ID token of every request except the health check (SR-02, ADR-04). A missing
 * header is left to the security entry point (401 {@code auth.token_missing}); an invalid token is
 * answered here (401 {@code auth.token_invalid}). A valid token gives the User of the request,
 * created on the first token if Firebase still holds the account (R-001-11, R-021-05); an e-mail
 * held by another identifier is 409 {@code user.email_taken} (D-18d). Nothing about the token is
 * logged.
 */
public class FirebaseTokenFilter extends OncePerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(FirebaseTokenFilter.class);
  private static final String BEARER = "Bearer ";

  private final TokenVerifier verifier;
  private final UserService users;
  private final AccountDirectory directory;

  public FirebaseTokenFilter(
      TokenVerifier verifier, UserService users, AccountDirectory directory) {
    this.verifier = verifier;
    this.users = users;
    this.directory = directory;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return HealthCheckPath.matches(request);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header == null || !header.startsWith(BEARER)) {
      chain.doFilter(request, response);
      return;
    }
    try {
      VerifiedToken token = verifier.verify(header.substring(BEARER.length()).trim());
      User known = users.findOrCreate(token, directory::exists);
      AuthenticatedUser user =
          new AuthenticatedUser(known.id(), known.email(), token.emailVerified());
      SecurityContextHolder.getContext()
          .setAuthentication(
              UsernamePasswordAuthenticationToken.authenticated(
                  user, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    } catch (InvalidTokenException e) {
      ErrorWriter.write(
          response,
          HttpStatus.UNAUTHORIZED.value(),
          "auth.token_invalid",
          "Invalid or expired token");
      return;
    } catch (EmailTakenException e) {
      LOG.info("409 e-mail taken on {} {}", request.getMethod(), request.getRequestURI());
      ErrorWriter.write(
          response,
          HttpStatus.CONFLICT.value(),
          "user.email_taken",
          "E-mail belongs to another user");
      return;
    }
    chain.doFilter(request, response);
  }
}
