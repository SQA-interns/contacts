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

/**
 * Checks the Firebase ID token of every request except the health check (SR-02, ADR-04). A missing
 * header is left to the security entry point (401 {@code auth.token_missing}); an invalid token is
 * answered here (401 {@code auth.token_invalid}). Nothing about the token is logged.
 */
public class FirebaseTokenFilter extends OncePerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(FirebaseTokenFilter.class);
  private static final String BEARER = "Bearer ";

  private final TokenVerifier verifier;

  public FirebaseTokenFilter(TokenVerifier verifier) {
    this.verifier = verifier;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.equals("/health") || path.startsWith("/health/");
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
      AuthenticatedUser user = AuthenticatedUser.from(token);
      SecurityContextHolder.getContext()
          .setAuthentication(
              UsernamePasswordAuthenticationToken.authenticated(
                  user, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    } catch (InvalidTokenException e) {
      LOG.info("401 invalid token on {} {}", request.getMethod(), request.getRequestURI());
      ErrorWriter.write(
          response,
          HttpStatus.UNAUTHORIZED.value(),
          "auth.token_invalid",
          "Invalid or expired token");
      return;
    }
    chain.doFilter(request, response);
  }
}
