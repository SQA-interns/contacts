package si.contactgroups.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import si.contactgroups.domain.AttributePair;
import si.contactgroups.domain.ContactChanges;
import si.contactgroups.domain.ContactData;
import si.contactgroups.error.ApiError.FieldError;

/**
 * The values of a merge (US-026): the chosen fields and Attribute values must be values of the
 * merged Contacts (D-19 d), the Tags are combined (R-026-03) and so are the Attributes with a
 * choice for a key that clashes (R-026-04). The first source is the Survivor.
 */
final class MergeValues {

  private static final String INVALID = "merge.invalid_value";

  private MergeValues() {}

  /**
   * The chosen fields, trimmed. An empty choice is always allowed; any other value must be the
   * value of the same field of one source (the e-mail ignoring case, as it is stored in lower
   * case).
   *
   * @throws MergeValueException naming every field whose value no source has
   */
  static ContactData chooseFields(ContactData chosen, List<MergeSource> sources) {
    Map<String, String> wanted = ContactChanges.fields(chosen);
    List<Map<String, String>> available =
        sources.stream().map(source -> ContactChanges.fields(source.data())).toList();
    List<FieldError> errors = new ArrayList<>();
    Map<String, String> kept = new LinkedHashMap<>();
    wanted.forEach(
        (field, value) -> {
          String text = value == null ? "" : value.strip();
          if (!text.isEmpty()
              && available.stream().noneMatch(v -> same(field, text, v.get(field)))) {
            errors.add(new FieldError(field, INVALID));
          }
          kept.put(field, text.isEmpty() ? null : text);
        });
    if (!errors.isEmpty()) {
      throw new MergeValueException(errors);
    }
    return new ContactData(
        kept.get("firstName"),
        kept.get("lastName"),
        kept.get("email"),
        kept.get("phone"),
        kept.get("company"),
        kept.get("jobTitle"),
        kept.get("street"),
        kept.get("postalCode"),
        kept.get("city"),
        kept.get("country"),
        kept.get("notes"));
  }

  private static boolean same(String field, String chosen, String stored) {
    return "email".equals(field) ? chosen.equalsIgnoreCase(stored) : chosen.equals(stored);
  }

  /** The Tags of all sources without repeats ignoring case, the Survivor's first (R-026-03). */
  static List<String> combineTags(List<MergeSource> sources) {
    Map<String, String> combined = new LinkedHashMap<>();
    sources.stream()
        .flatMap(source -> source.tags().stream())
        .forEach(tag -> combined.putIfAbsent(key(tag), tag));
    return List.copyOf(combined.values());
  }

  /**
   * The Attributes of all sources by key ignoring case, with the spelling of the first source that
   * has the key. A key with one value in all sources keeps it; a key with several values takes the
   * one {@code chosen} value (R-026-04).
   *
   * @throws MergeValueException when a key with several values has no choice, or a choice is not a
   *     value of that key
   */
  static List<AttributePair> combineAttributes(
      List<AttributePair> chosen, List<MergeSource> sources) {
    Map<String, AttributePair> first = new LinkedHashMap<>();
    Map<String, Set<String>> values = new LinkedHashMap<>();
    for (MergeSource source : sources) {
      for (AttributePair pair : source.attributes()) {
        first.putIfAbsent(key(pair.key()), pair);
        values.computeIfAbsent(key(pair.key()), k -> new LinkedHashSet<>()).add(pair.value());
      }
    }
    Map<String, String> choices = new LinkedHashMap<>();
    boolean valid = true;
    for (AttributePair pair : chosen) {
      String key = key(pair.key());
      valid &= values.containsKey(key) && values.get(key).contains(pair.value());
      valid &= choices.putIfAbsent(key, pair.value()) == null;
    }
    List<AttributePair> combined = new ArrayList<>();
    for (Map.Entry<String, Set<String>> entry : values.entrySet()) {
      String value = valueOf(entry.getValue(), choices.get(entry.getKey()));
      valid &= value != null;
      combined.add(new AttributePair(first.get(entry.getKey()).key(), value));
    }
    if (!valid) {
      throw new MergeValueException(List.of(new FieldError("attributes", INVALID)));
    }
    return combined;
  }

  private static String valueOf(Set<String> all, String choice) {
    if (all.size() == 1) {
      return all.iterator().next();
    }
    return choice;
  }

  private static String key(String text) {
    return text.toLowerCase(Locale.ROOT);
  }
}
