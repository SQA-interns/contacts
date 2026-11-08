package si.contactgroups.service;

import java.util.List;
import si.contactgroups.error.ApiError.FieldError;

/** Input that breaks a rule of the data model; answered with 422 {@code validation.failed}. */
public class ValidationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final transient List<FieldError> fieldErrors;

  public ValidationException(List<FieldError> fieldErrors) {
    super("validation failed");
    this.fieldErrors = List.copyOf(fieldErrors);
  }

  public List<FieldError> getFieldErrors() {
    return fieldErrors;
  }
}
