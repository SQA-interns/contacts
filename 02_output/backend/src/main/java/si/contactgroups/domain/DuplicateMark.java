package si.contactgroups.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

/** How sure the search is that the Contacts of a Duplicate set are one person (R-024-02..03). */
public enum DuplicateMark {
  CERTAIN,
  PROBABLE;

  /** The word of the contract enum {@code DuplicateSet.mark}. */
  @JsonValue
  public String word() {
    return name().toLowerCase(Locale.ROOT);
  }
}
