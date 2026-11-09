package si.contactgroups.domain;

import java.util.List;

/** Contract schema {@code Facets}: the values of the filters that occur in a Group (R-009-11). */
public record Facets(
    List<String> tags,
    List<String> companies,
    List<String> countries,
    List<String> attributeKeys) {}
