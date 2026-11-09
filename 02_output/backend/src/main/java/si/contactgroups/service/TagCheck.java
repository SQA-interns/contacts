package si.contactgroups.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import si.contactgroups.error.ApiError.FieldError;

/** R-005-02, R-005-04: trims the Tag names and reports every broken rule. */
final class TagCheck {

  static final int MAX_TAGS = 50;
  private static final Pattern NAME = Pattern.compile("^[\\p{L}\\p{N}_-]{1,50}$");

  private TagCheck() {}

  /**
   * @param names the sent names; {@code null} counts as none
   * @param errors receives {@code tags[i]} for an invalid name and {@code tags} for the limit
   * @return the trimmed names without repeats (ignoring case)
   */
  static List<String> clean(List<String> names, List<FieldError> errors) {
    List<String> cleaned = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    List<String> sent = names == null ? List.of() : names;
    for (int i = 0; i < sent.size(); i++) {
      String name = sent.get(i) == null ? "" : sent.get(i).strip();
      if (!NAME.matcher(name).matches()) {
        errors.add(new FieldError("tags[" + i + "]", "validation.tag_invalid"));
      } else if (seen.add(name.toLowerCase(Locale.ROOT))) {
        cleaned.add(name);
      }
    }
    if (cleaned.size() > MAX_TAGS) {
      errors.add(new FieldError("tags", "validation.tag_limit"));
    }
    return cleaned;
  }
}
