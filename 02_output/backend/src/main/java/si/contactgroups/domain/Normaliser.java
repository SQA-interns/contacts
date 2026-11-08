package si.contactgroups.domain;

import java.text.Normalizer;
import java.util.Locale;

/** The comparison text of BR-05 and ADR-06: no accents, no case, trimmed. */
public final class Normaliser {

  private Normaliser() {}

  /** The normalised text; {@code null} stays {@code null}. */
  public static String normalise(String text) {
    if (text == null) {
      return null;
    }
    String decomposed = Normalizer.normalize(text.strip(), Normalizer.Form.NFD);
    return decomposed
        .replaceAll("\\p{M}+", "")
        .replace("đ", "d")
        .replace("Đ", "d")
        .replace("ß", "ss")
        .toLowerCase(Locale.ROOT);
  }
}
