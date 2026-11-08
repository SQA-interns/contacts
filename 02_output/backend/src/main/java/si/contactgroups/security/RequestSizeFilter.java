package si.contactgroups.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.util.unit.DataSize;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Refuses a request body larger than the limit with 413 {@code request.too_large} before anything
 * is read or stored (R-018-11, SR-08, ADR-11). It is the first filter of the chain.
 */
public class RequestSizeFilter extends OncePerRequestFilter {

  private final long maxBytes;

  public RequestSizeFilter(DataSize limit) {
    this.maxBytes = limit.toBytes();
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (request.getContentLengthLong() > maxBytes) {
      ErrorWriter.write(
          response,
          HttpStatus.PAYLOAD_TOO_LARGE.value(),
          "request.too_large",
          "The request body is too large");
      return;
    }
    chain.doFilter(request, response);
  }
}
