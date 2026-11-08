package si.contactgroups.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import si.contactgroups.error.ApiError;
import si.contactgroups.security.RequestTooLargeException;
import si.contactgroups.service.GroupNameTakenException;
import si.contactgroups.service.GroupNotFoundException;
import si.contactgroups.service.RoleForbiddenException;
import si.contactgroups.service.ValidationException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

/**
 * The only place that turns a failure into the error format (ADR-10, SR-12): no stack trace, no
 * SQL.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
  public ResponseEntity<ApiError> notFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ApiError("route.not_found", "Not found"));
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiError> methodNotAllowed() {
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
        .body(new ApiError("request.method_not_allowed", "Method not allowed"));
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ApiError> unsupportedMediaType() {
    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        .body(new ApiError("request.unsupported_media_type", "Unsupported media type"));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> malformed(HttpMessageNotReadableException e) {
    if (e.getMostSpecificCause() instanceof RequestTooLargeException tooLarge) {
      return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
          .body(new ApiError(RequestTooLargeException.CODE, tooLarge.getMessage()));
    }
    if (e.getCause() instanceof UnrecognizedPropertyException) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(new ApiError("request.unknown_property", "Unknown property in the request body"));
    }
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ApiError("request.malformed", "Malformed request"));
  }

  @ExceptionHandler(GroupNotFoundException.class)
  public ResponseEntity<ApiError> groupNotFound(GroupNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ApiError("group.not_found", e.getMessage()));
  }

  @ExceptionHandler(RoleForbiddenException.class)
  public ResponseEntity<ApiError> roleForbidden(RoleForbiddenException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new ApiError("auth.role_forbidden", e.getMessage()));
  }

  @ExceptionHandler(GroupNameTakenException.class)
  public ResponseEntity<ApiError> groupNameTaken(GroupNameTakenException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ApiError("group.name_taken", e.getMessage()));
  }

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ApiError> validationFailed(ValidationException e) {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
        .body(new ApiError("validation.failed", "Validation failed", e.getFieldErrors()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> unexpected(Exception e) {
    LOG.error("unexpected error of type {}", e.getClass().getName());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ApiError("server.error", "Unexpected error"));
  }
}
