package si.contactgroups.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** What a save did to the Attributes of a Contact, matched by key ignoring case (R-004-06). */
public final class AttributeChanges {

  /** One added, changed or removed pair; the missing side is {@code null}. */
  public record Change(HistoryAction action, String key, String oldValue, String newValue) {}

  private AttributeChanges() {}

  /** Changed and added pairs in the new order, then removed pairs in the old order. */
  public static List<Change> between(List<AttributePair> before, List<AttributePair> after) {
    Map<String, AttributePair> old = byKey(before);
    Map<String, AttributePair> now = byKey(after);
    List<Change> changes = new ArrayList<>();
    now.forEach(
        (lower, pair) -> {
          AttributePair was = old.get(lower);
          if (was == null) {
            changes.add(new Change(HistoryAction.ATTRIBUTE_ADDED, pair.key(), null, pair.value()));
          } else if (!Objects.equals(was.value(), pair.value())) {
            changes.add(
                new Change(HistoryAction.ATTRIBUTE_CHANGED, pair.key(), was.value(), pair.value()));
          }
        });
    old.forEach(
        (lower, pair) -> {
          if (!now.containsKey(lower)) {
            changes.add(
                new Change(HistoryAction.ATTRIBUTE_REMOVED, pair.key(), pair.value(), null));
          }
        });
    return changes;
  }

  private static Map<String, AttributePair> byKey(List<AttributePair> pairs) {
    Map<String, AttributePair> map = new LinkedHashMap<>();
    pairs.forEach(p -> map.put(p.key().toLowerCase(Locale.ROOT), p));
    return map;
  }
}
