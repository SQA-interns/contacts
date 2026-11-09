package si.contactgroups.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

/** The columns, cells and file name of the Export file (R-011-03, R-011-04, R-011-09). */
public final class ExportColumns {

  private static final List<String> HEADER_KEYS =
      List.of(
          "firstName",
          "lastName",
          "email",
          "phone",
          "company",
          "jobTitle",
          "street",
          "postalCode",
          "city",
          "country",
          "notes",
          "tags");

  private static final Comparator<String> BY_NAME =
      Comparator.comparing(Normaliser::normalise).thenComparing(Comparator.naturalOrder());

  private ExportColumns() {}

  /** The 11 field headers and {@code Tags} in the language ({@code sl} or {@code en}). */
  public static List<String> header(String lang) {
    ResourceBundle texts = ResourceBundle.getBundle("export", Locale.forLanguageTag(lang));
    return HEADER_KEYS.stream().map(texts::getString).toList();
  }

  /**
   * One column per Attribute key used by the Contacts, keys differing only in case in one column,
   * ordered ignoring case and accents (a key with an accent is another key). The column shows the
   * smallest spelling of the key.
   */
  public static List<String> attributeKeys(List<ExportContact> contacts) {
    Map<String, String> byKey = new TreeMap<>(BY_NAME);
    for (ExportContact contact : contacts) {
      for (AttributePair pair : contact.attributes()) {
        byKey.merge(pair.key().toLowerCase(Locale.ROOT), pair.key(), ExportColumns::smaller);
      }
    }
    return new ArrayList<>(byKey.values());
  }

  private static String smaller(String a, String b) {
    return a.compareTo(b) <= 0 ? a : b;
  }

  /** The cells of one Contact: fields, Tags, then the value of each Attribute column. */
  public static List<String> row(ExportContact contact, List<String> attributeKeys, String lang) {
    ContactData d = contact.data();
    Locale locale = Locale.forLanguageTag(lang);
    List<String> cells = new ArrayList<>();
    for (String field :
        new String[] {
          d.firstName(),
          d.lastName(),
          d.email(),
          d.phone(),
          d.company(),
          d.jobTitle(),
          d.street(),
          d.postalCode(),
          d.city(),
          countryName(d.country(), locale),
          d.notes()
        }) {
      cells.add(field == null ? "" : field);
    }
    cells.add(String.join(", ", contact.tags().stream().sorted(BY_NAME).toList()));
    for (String key : attributeKeys) {
      cells.add(valueOf(contact, key));
    }
    return cells;
  }

  private static String countryName(String code, Locale locale) {
    return code == null ? null : Locale.of("", code).getDisplayCountry(locale);
  }

  private static String valueOf(ExportContact contact, String key) {
    return contact.attributes().stream()
        .filter(pair -> pair.key().equalsIgnoreCase(key))
        .map(AttributePair::value)
        .findFirst()
        .orElse("");
  }

  /**
   * {@code <Group name>_<YYYY-MM-DD>.xlsx}; characters not allowed in file names become {@code _}.
   */
  public static String fileName(String groupName, LocalDate day) {
    return groupName.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_") + "_" + day + ".xlsx";
  }
}
