package si.contactgroups.service;

import java.util.List;
import si.contactgroups.error.ApiError.FieldError;

/**
 * A chosen value of a merge that no merged Contact has, or a clash of Attribute values without a
 * choice; answered with 422 {@code merge.invalid_value} (D-19 d, R-026-04).
 */
public class MergeValueException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final transient List<FieldError> fieldErrors;

  public MergeValueException(List<FieldError> fieldErrors) {
    super("a merged value is none of the values of the merged Contacts");
    this.fieldErrors = List.copyOf(fieldErrors);
  }

  public List<FieldError> getFieldErrors() {
    return fieldErrors;
  }
}
