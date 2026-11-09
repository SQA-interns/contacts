package si.contactgroups.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** One row of the Contact list (contract schema {@code ContactListItem}). */
public record ContactListItem(
    UUID id,
    String firstName,
    String lastName,
    String email,
    String company,
    String city,
    String country,
    List<String> tags,
    int version,
    Instant createdAt) {}
