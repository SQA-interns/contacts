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
import si.contactgroups.service.ActivityRangeException;
import si.contactgroups.service.ContactChangedException;
import si.contactgroups.service.ContactInTrashException;
import si.contactgroups.service.ContactNotFoundException;
import si.contactgroups.service.ExportTooManyException;
import si.contactgroups.service.GroupNameMismatchException;
import si.contactgroups.service.GroupNameTakenException;
import si.contactgroups.service.GroupNotFoundException;
import si.contactgroups.service.InvalidQueryException;
import si.contactgroups.service.InvitationExistsException;
import si.contactgroups.service.InvitationLimitException;
import si.contactgroups.service.InvitationNotFoundException;
import si.contactgroups.service.LastOwnerException;
import si.contactgroups.service.MemberNotFoundException;
import si.contactgroups.service.RoleForbiddenException;
import si.contactgroups.service.SoleOwnerException;
import si.contactgroups.service.TagNameTakenException;
import si.contactgroups.service.TagNotFoundException;
import si.contactgroups.service.ValidationException;
import si.contactgroups.service.VersionMissingException;
import si.contactgroups.service.VersionNotFoundException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

/**
 * The only place that turns a failure into the error format (ADR-10, SR-12): no stack trace, no
 * SQL.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
  public ResponseEntity<ApiError> notFound(jakarta.servlet.http.HttpServletRequest request) {
    if (isWriteOnHistory(request)) {
      return methodNotAllowed();
    }
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

  @ExceptionHandler(GroupNameMismatchException.class)
  public ResponseEntity<ApiError> groupNameMismatch(GroupNameMismatchException e) {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
        .body(new ApiError("group.name_mismatch", e.getMessage()));
  }

  @ExceptionHandler(InvitationExistsException.class)
  public ResponseEntity<ApiError> invitationExists(InvitationExistsException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ApiError("invitation.exists", e.getMessage()));
  }

  @ExceptionHandler(InvitationLimitException.class)
  public ResponseEntity<ApiError> invitationLimit(InvitationLimitException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ApiError("invitation.limit", e.getMessage()));
  }

  @ExceptionHandler(InvitationNotFoundException.class)
  public ResponseEntity<ApiError> invitationNotFound(InvitationNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ApiError("invitation.not_found", e.getMessage()));
  }

  @ExceptionHandler(MemberNotFoundException.class)
  public ResponseEntity<ApiError> memberNotFound(MemberNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ApiError("member.not_found", e.getMessage()));
  }

  @ExceptionHandler(SoleOwnerException.class)
  public ResponseEntity<SoleOwnerConflict> soleOwner(SoleOwnerException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new SoleOwnerConflict("account.sole_owner", e.getMessage(), e.groups()));
  }

  @ExceptionHandler(LastOwnerException.class)
  public ResponseEntity<ApiError> lastOwner(LastOwnerException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ApiError("group.last_owner", e.getMessage()));
  }

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ApiError> validationFailed(ValidationException e) {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
        .body(new ApiError("validation.failed", "Validation failed", e.getFieldErrors()));
  }

  @ExceptionHandler(ContactNotFoundException.class)
  public ResponseEntity<ApiError> contactNotFound(ContactNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ApiError("contact.not_found", e.getMessage()));
  }

  @ExceptionHandler(ContactInTrashException.class)
  public ResponseEntity<ApiError> contactInTrash(ContactInTrashException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ApiError("contact.in_trash", e.getMessage()));
  }

  @ExceptionHandler(ContactChangedException.class)
  public ResponseEntity<ContactConflict> contactChanged(ContactChangedException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            new ContactConflict("contact.changed_by_someone_else", e.getMessage(), e.getCurrent()));
  }

  @ExceptionHandler(VersionNotFoundException.class)
  public ResponseEntity<ApiError> versionNotFound(VersionNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ApiError("version.not_found", e.getMessage()));
  }

  @ExceptionHandler(TagNotFoundException.class)
  public ResponseEntity<ApiError> tagNotFound(TagNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ApiError("tag.not_found", e.getMessage()));
  }

  @ExceptionHandler(TagNameTakenException.class)
  public ResponseEntity<ApiError> tagNameTaken(TagNameTakenException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ApiError("tag.name_taken", e.getMessage()));
  }

  @ExceptionHandler(InvalidQueryException.class)
  public ResponseEntity<ApiError> invalidQuery(InvalidQueryException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ApiError("request.malformed", e.getMessage()));
  }

  @ExceptionHandler(ActivityRangeException.class)
  public ResponseEntity<ApiError> activityRange(ActivityRangeException e) {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
        .body(new ApiError("activity.range_invalid", e.getMessage()));
  }

  @ExceptionHandler(ExportTooManyException.class)
  public ResponseEntity<ApiError> exportTooMany(ExportTooManyException e) {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
        .body(new ApiError("export.too_many", e.getMessage()));
  }

  @ExceptionHandler(VersionMissingException.class)
  public ResponseEntity<ApiError> versionMissing(VersionMissingException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ApiError("request.malformed", e.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> unexpected(Exception e) {
    LOG.error("unexpected error of type {}", e.getClass().getName());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ApiError("server.error", "Unexpected error"));
  }

  /** No History entry can be changed or deleted, so such a request is a wrong method (R-016-05). */
  private static boolean isWriteOnHistory(jakarta.servlet.http.HttpServletRequest request) {
    String method = request.getMethod();
    return !"GET".equals(method)
        && !"HEAD".equals(method)
        && !"OPTIONS".equals(method)
        && request.getRequestURI().matches(".*/contacts/[^/]+/history/.*");
  }
}
