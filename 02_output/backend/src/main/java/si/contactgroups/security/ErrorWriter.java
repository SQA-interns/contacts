package si.contactgroups.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import si.contactgroups.error.ApiError;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes the error format (ADR-10) from servlet filters, where the exception handler does not
 * reach.
 */
public final class ErrorWriter {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  private ErrorWriter() {}

  /** Serialises an {@link ApiError}; the texts are fixed strings, never user input. */
  public static void write(HttpServletResponse response, int status, String code, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    MAPPER.writeValue(response.getOutputStream(), new ApiError(code, message));
  }
}
