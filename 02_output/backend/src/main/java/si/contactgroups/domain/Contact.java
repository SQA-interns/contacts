package si.contactgroups.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A Contact as the REST interface shows it (contract schema {@code Contact}): the 11 predefined
 * fields flat, plus version number, Tags and Attributes. Empty fields are left out.
 */
public record Contact(
    UUID id,
    int version,
    @JsonUnwrapped @JsonInclude(JsonInclude.Include.NON_NULL) ContactData data,
    List<String> tags,
    List<AttributePair> attributes,
    Instant createdAt,
    Instant updatedAt) {}
