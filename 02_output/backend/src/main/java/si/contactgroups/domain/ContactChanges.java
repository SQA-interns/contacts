package si.contactgroups.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** What a save changed in the 11 predefined fields (R-016-01, R-016-02). */
public final class ContactChanges {

  /** The value before and after; an empty field is {@code null}. */
  public record Change(String oldValue, String newValue) {}

  private ContactChanges() {}

  /** The fields whose value differs, in field order; empty when nothing changed. */
  public static Map<String, Change> between(ContactData before, ContactData after) {
    Map<String, String> old = fields(before);
    Map<String, Change> changes = new LinkedHashMap<>();
    fields(after)
        .forEach(
            (name, value) -> {
              if (!Objects.equals(old.get(name), value)) {
                changes.put(name, new Change(old.get(name), value));
              }
            });
    return changes;
  }

  /** The fields that have a value, in field order. */
  public static Map<String, String> stored(ContactData data) {
    Map<String, String> values = new LinkedHashMap<>();
    fields(data)
        .forEach(
            (name, value) -> {
              if (value != null) {
                values.put(name, value);
              }
            });
    return values;
  }

  private static Map<String, String> fields(ContactData d) {
    Map<String, String> m = new LinkedHashMap<>();
    m.put("firstName", d.firstName());
    m.put("lastName", d.lastName());
    m.put("email", d.email());
    m.put("phone", d.phone());
    m.put("company", d.company());
    m.put("jobTitle", d.jobTitle());
    m.put("street", d.street());
    m.put("postalCode", d.postalCode());
    m.put("city", d.city());
    m.put("country", d.country());
    m.put("notes", d.notes());
    return m;
  }
}
