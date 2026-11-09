package si.contactgroups.service;

import java.util.List;
import si.contactgroups.domain.ContactFilter;

/**
 * The search and filter query parameters of the Contact list as sent (contract {@code
 * ContactFilter}, US-008, US-009).
 */
public record FilterParams(
    String q,
    List<String> tags,
    String tagMode,
    List<String> withoutTags,
    String country,
    String company,
    String attrKey,
    String attrValue,
    String attrExists) {

  /**
   * @throws InvalidQueryException when a parameter is not allowed by the contract
   */
  public ContactFilter toFilter() {
    return FilterParser.parse(
        q, tags, tagMode, withoutTags, country, company, attrKey, attrValue, attrExists);
  }
}
