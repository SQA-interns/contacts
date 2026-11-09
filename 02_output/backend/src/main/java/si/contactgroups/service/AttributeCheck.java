package si.contactgroups.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import si.contactgroups.domain.AttributePair;
import si.contactgroups.error.ApiError.FieldError;

/** R-004-02..04: trims the Attributes and reports every broken rule next to its pair. */
final class AttributeCheck {

  static final int MAX_PAIRS = 50;
  private static final int MAX_KEY = 50;
  private static final int MAX_VALUE = 255;

  private AttributeCheck() {}

  /**
   * @param pairs the sent pairs; {@code null} counts as none
   * @param errors receives one error per broken rule
   * @return the trimmed pairs
   */
  static List<AttributePair> clean(List<AttributePair> pairs, List<FieldError> errors) {
    List<AttributePair> cleaned = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    List<AttributePair> sent = pairs == null ? List.of() : pairs;
    for (int i = 0; i < sent.size(); i++) {
      AttributePair pair = trimmed(sent.get(i));
      String at = "attributes[" + i + "]";
      if (pair.key().isEmpty() || pair.key().length() > MAX_KEY) {
        errors.add(new FieldError(at + ".key", "validation.attribute_key_invalid"));
      } else if (!seen.add(pair.key().toLowerCase(Locale.ROOT))) {
        errors.add(new FieldError(at + ".key", "validation.attribute_duplicate_key"));
      }
      if (pair.value().isEmpty() || pair.value().length() > MAX_VALUE) {
        errors.add(new FieldError(at + ".value", "validation.attribute_value_invalid"));
      }
      cleaned.add(pair);
    }
    if (sent.size() > MAX_PAIRS) {
      errors.add(new FieldError("attributes", "validation.attribute_limit"));
    }
    return cleaned;
  }

  private static AttributePair trimmed(AttributePair pair) {
    return new AttributePair(strip(pair.key()), strip(pair.value()));
  }

  private static String strip(String text) {
    return text == null ? "" : text.strip();
  }
}
