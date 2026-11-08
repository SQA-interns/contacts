package si.contactgroups.security;

import jakarta.servlet.http.HttpServletRequest;

/** The one place that knows which paths belong to the public health check (R-018-07). */
final class HealthCheckPath {

  private HealthCheckPath() {}

  static boolean matches(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.equals("/health") || path.startsWith("/health/");
  }
}
