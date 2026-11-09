package si.contactgroups.domain;

import com.fasterxml.jackson.annotation.JsonRawValue;
import java.time.Instant;
import java.util.UUID;

/**
 * A History entry as the REST interface shows it (contract schema {@code HistoryEntry}); {@code
 * details} is JSON text with a fixed shape per action (ADR-08).
 */
public record HistoryEntry(
    UUID id,
    Instant occurredAt,
    String userName,
    String userEmail,
    HistoryAction action,
    UUID contactId,
    @JsonRawValue String details) {}
