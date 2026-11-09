package si.contactgroups.domain;

import java.util.UUID;

/** The id of a History entry with its details as JSON text. */
public record HistoryDetails(UUID id, String details) {}
