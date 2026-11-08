package si.contactgroups.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** The error format of the REST interface (ADR-10, docs/02_contracts/openapi.yaml "Error"). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String code, String message, List<FieldError> fieldErrors) {

  public ApiError(String code, String message) {
    this(code, message, null);
  }

  /** One field in error; both values are codes the UI translates. */
  public record FieldError(String field, String code) {}
}
