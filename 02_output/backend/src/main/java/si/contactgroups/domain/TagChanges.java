package si.contactgroups.domain;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Which Tags a save put on a Contact and which it took off, matched ignoring case (R-005-03). */
public record TagChanges(List<String> added, List<String> removed) {

  /** Added names keep the typed spelling, removed names the stored one; both keep their order. */
  public static TagChanges between(List<String> before, List<String> after) {
    Set<String> old = lower(before);
    Set<String> now = lower(after);
    return new TagChanges(
        after.stream().filter(n -> !old.contains(key(n))).toList(),
        before.stream().filter(n -> !now.contains(key(n))).toList());
  }

  public boolean isEmpty() {
    return added.isEmpty() && removed.isEmpty();
  }

  private static Set<String> lower(List<String> names) {
    return names.stream().map(TagChanges::key).collect(Collectors.toSet());
  }

  private static String key(String name) {
    return name.toLowerCase(Locale.ROOT);
  }
}
