package si.contactgroups.service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import si.contactgroups.domain.ContactFilter;
import si.contactgroups.domain.Normaliser;
import si.contactgroups.domain.SearchWords;

/** Turns the query parameters of the Contact list into a {@link ContactFilter} (US-008, US-009). */
final class FilterParser {

  private static final Pattern COUNTRY_CODE = Pattern.compile("[A-Z]{2}");

  private FilterParser() {}

  /**
   * @throws InvalidQueryException when a parameter is not allowed by the contract
   */
  static ContactFilter parse(
      String q,
      List<String> tags,
      String tagMode,
      List<String> withoutTags,
      String country,
      String company,
      String attrKey,
      String attrValue,
      String attrExists) {
    boolean anyTag = anyTag(tagMode);
    String key = blankToNull(attrKey) == null ? null : attrKey.strip().toLowerCase(Locale.ROOT);
    String value = blankToNull(attrValue) == null ? null : Normaliser.normalise(attrValue);
    boolean exists = exists(attrExists);
    boolean byAttribute = key != null && (value != null || exists);
    return new ContactFilter(
        SearchWords.patterns(q),
        names(tags),
        anyTag,
        names(withoutTags),
        country(country),
        blankToNull(company) == null ? null : Normaliser.normalise(company),
        byAttribute ? key : null,
        byAttribute ? value : null,
        byAttribute && value == null);
  }

  private static boolean anyTag(String tagMode) {
    if (tagMode == null || "all".equals(tagMode)) {
      return false;
    }
    if ("any".equals(tagMode)) {
      return true;
    }
    throw new InvalidQueryException("Tag mode must be all or any");
  }

  private static boolean exists(String attrExists) {
    if (attrExists == null || "false".equals(attrExists)) {
      return false;
    }
    if ("true".equals(attrExists)) {
      return true;
    }
    throw new InvalidQueryException("attrExists must be true or false");
  }

  private static String country(String country) {
    if (blankToNull(country) == null) {
      return null;
    }
    if (!COUNTRY_CODE.matcher(country).matches()) {
      throw new InvalidQueryException("Country must be two capital letters");
    }
    return country;
  }

  private static List<String> names(List<String> names) {
    if (names == null) {
      return List.of();
    }
    return names.stream().filter(n -> blankToNull(n) != null).map(Normaliser::normalise).toList();
  }

  private static String blankToNull(String text) {
    return text == null || text.isBlank() ? null : text;
  }
}
