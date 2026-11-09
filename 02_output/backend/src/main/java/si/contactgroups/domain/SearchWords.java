package si.contactgroups.domain;

import java.util.Arrays;
import java.util.List;

/** The search text of US-008 as {@code LIKE ... ESCAPE '\'} patterns, one per word (ADR-06). */
public final class SearchWords {

  private SearchWords() {}

  /**
   * One pattern per word of the normalised text; {@code %}, {@code _} and {@code \} are escaped so
   * that they match as plain text (R-008-06). Empty or blank text gives no pattern (R-008-05).
   */
  public static List<String> patterns(String text) {
    String normalised = Normaliser.normalise(text);
    if (normalised == null || normalised.isEmpty()) {
      return List.of();
    }
    return Arrays.stream(normalised.split("\\s+")).map(SearchWords::pattern).toList();
  }

  private static String pattern(String word) {
    return "%" + word.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
  }
}
