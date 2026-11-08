package si.contactgroups.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/** Refuses a request that did not arrive over HTTPS (R-018-12, SB-04, ADR-11). */
public class HttpsFilter extends OncePerRequestFilter {

  private final boolean required;

  public HttpsFilter(boolean required) {
    this.required = required;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (required && !isSecure(request) && !HealthCheckPath.matches(request)) {
      ErrorWriter.write(
          response, HttpStatus.FORBIDDEN.value(), "https.required", "HTTPS is required");
      return;
    }
    chain.doFilter(request, response);
  }

  /** Behind the reverse proxy the forwarded protocol decides (the forwarded-header filter ran). */
  private static boolean isSecure(HttpServletRequest request) {
    return request.isSecure() || "https".equalsIgnoreCase(request.getScheme());
  }
}
