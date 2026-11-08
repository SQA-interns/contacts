package si.contactgroups.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * While the e-mail of the caller is not verified, every request about a Group is answered 403
 * {@code auth.email_not_verified} (R-001-04, R-018-06); it runs after the token check (D-18c).
 */
public class EmailVerifiedFilter extends OncePerRequestFilter {

  private static final String GROUPS = "/api/v1/groups";
  private static final String GROUP_TRASH = "/api/v1/group-trash";

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return !(isUnder(path, GROUPS) || isUnder(path, GROUP_TRASH));
  }

  private static boolean isUnder(String path, String root) {
    return path.equals(root) || path.startsWith(root + "/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (AuthenticatedUser.current().filter(caller -> !caller.emailVerified()).isPresent()) {
      ErrorWriter.write(
          response, HttpStatus.FORBIDDEN.value(), "auth.email_not_verified", "E-mail not verified");
      return;
    }
    chain.doFilter(request, response);
  }
}
