package si.contactgroups.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
    chain.doFilter(
        request.getContentLengthLong() < 0 ? new CountingRequest(request) : request, response);
  }

  /** A request whose body fails with {@link RequestTooLargeException} once it passes the limit. */
  private final class CountingRequest extends HttpServletRequestWrapper {

    CountingRequest(HttpServletRequest request) {
      super(request);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
      return new CountingStream(super.getInputStream());
    }

    @Override
    public BufferedReader getReader() throws IOException {
      String encoding = getCharacterEncoding();
      Charset charset = encoding == null ? StandardCharsets.ISO_8859_1 : Charset.forName(encoding);
      return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }
  }

  private final class CountingStream extends ServletInputStream {

    private final ServletInputStream source;
    private long read;

    CountingStream(ServletInputStream source) {
      this.source = source;
    }

    @Override
    public int read() throws IOException {
      int next = source.read();
      if (next >= 0) {
        count(1);
      }
      return next;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
      int n = source.read(buffer, offset, length);
      if (n > 0) {
        count(n);
      }
      return n;
    }

    private void count(int bytes) throws IOException {
      read += bytes;
      if (read > maxBytes) {
        throw new RequestTooLargeException();
      }
    }

    @Override
    public boolean isFinished() {
      return source.isFinished();
    }

    @Override
    public boolean isReady() {
      return source.isReady();
    }

    @Override
    public void setReadListener(ReadListener listener) {
      source.setReadListener(listener);
    }
  }
}
