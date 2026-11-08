package si.contactgroups.domain;

import java.util.Locale;
import java.util.Set;

/** The country list of BR-06: ISO 3166-1 alpha-2 codes in upper case. */
public final class CountryCodes {

  private static final Set<String> CODES = Set.of(Locale.getISOCountries());

  private CountryCodes() {}

  public static boolean contains(String code) {
    return code != null && CODES.contains(code);
  }
}
