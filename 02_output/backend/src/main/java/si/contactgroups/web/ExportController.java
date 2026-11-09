package si.contactgroups.web;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import si.contactgroups.domain.Action;
import si.contactgroups.security.AuthenticatedUser;
import si.contactgroups.security.RequiresAction;
import si.contactgroups.service.ExportFile;
import si.contactgroups.service.ExportService;
import si.contactgroups.service.FilterParams;
import si.contactgroups.service.InvalidQueryException;

/** The Excel file of a Selection (contract {@code exportContacts}, US-011). */
@RestController
@RequestMapping("/api/v1/groups/{groupId}/exports")
public class ExportController {

  private static final MediaType XLSX =
      MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  private final ExportService exports;

  public ExportController(ExportService exports) {
    this.exports = exports;
  }

  @PostMapping
  @RequiresAction(Action.EXPORT)
  public ResponseEntity<byte[]> exportContacts(
      @AuthenticationPrincipal AuthenticatedUser caller,
      @PathVariable UUID groupId,
      @RequestBody ExportRequest body) {
    String lang = body.language();
    Selection selection = body.selection();
    if (selection == null || selection.isAmbiguous()) {
      throw new InvalidQueryException("The selection names contactIds or a filter");
    }
    ExportFile file =
        selection.contactIds() != null
            ? exports.exportIds(groupId, caller.userId(), selection.ids(), lang)
            : exports.exportFilter(
                groupId, caller.userId(), selection.filter().toParams().toFilter(), lang);
    return ResponseEntity.ok()
        .contentType(XLSX)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build()
                .toString())
        .body(file.bytes());
  }

  /** Contract schema {@code ExportRequest}; {@code lang} is {@code sl} when missing. */
  public record ExportRequest(Selection selection, String lang) {

    String language() {
      if (lang == null) {
        return "sl";
      }
      if (!"sl".equals(lang) && !"en".equals(lang)) {
        throw new InvalidQueryException("Language must be sl or en");
      }
      return lang;
    }
  }

  /** Either the ids of the Contacts or a filter ("select all results"). */
  public record Selection(List<String> contactIds, Filter filter) {

    boolean isAmbiguous() {
      return (contactIds == null) == (filter == null);
    }

    List<UUID> ids() {
      try {
        return contactIds.stream().map(UUID::fromString).toList();
      } catch (IllegalArgumentException e) {
        throw new InvalidQueryException("A contact id is not valid");
      }
    }
  }

  /** Contract schema {@code ContactFilter}. */
  public record Filter(
      String q,
      List<String> tags,
      String tagMode,
      List<String> withoutTags,
      String country,
      String company,
      String attrKey,
      String attrValue,
      Boolean attrExists) {

    FilterParams toParams() {
      return new FilterParams(
          q,
          tags,
          tagMode,
          withoutTags,
          country,
          company,
          attrKey,
          attrValue,
          attrExists == null ? null : attrExists.toString());
    }
  }
}
