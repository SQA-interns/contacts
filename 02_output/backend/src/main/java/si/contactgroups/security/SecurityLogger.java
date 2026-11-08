package si.contactgroups.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Writes one log line for each request that fails authentication or is answered 401, 403, 404 or
 * 429 (R-018-13, SR-07, SB-07). A line holds the status, the method and the path, never the query
 * string, a body, a header or a token.
 */
public class SecurityLogger extends OncePerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(SecurityLogger.class);
  private static final Set<Integer> LOGGED = Set.of(401, 403, 404, 429);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    try {
      chain.doFilter(request, response);
    } finally {
      if (LOGGED.contains(response.getStatus())) {
        LOG.info(
            "security: answered {} to {} {}",
            response.getStatus(),
            request.getMethod(),
            request.getRequestURI());
      }
    }
  }
}
