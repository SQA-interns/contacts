package si.contactgroups.domain;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Which Duplicate sets a dismissal ("not a duplicate", US-025) hides. A set is hidden when all its
 * Contacts belong to one dismissed set: the exact set (R-025-02) and what is left of it after a
 * Contact was edited or deleted (EX-025-02). A set with a Contact that was not in the dismissed set
 * is new information and is shown (R-025-03).
 */
public final class DismissedSets {

  private DismissedSets() {}

  /** The sets that no dismissal hides, in their order. */
  public static List<DuplicateSet> hide(List<DuplicateSet> sets, Collection<Set<UUID>> dismissed) {
    return sets.stream().filter(set -> !hidden(set, dismissed)).toList();
  }

  private static boolean hidden(DuplicateSet set, Collection<Set<UUID>> dismissed) {
    List<UUID> ids = set.contacts().stream().map(ContactListItem::id).toList();
    return dismissed.stream().anyMatch(contacts -> contacts.containsAll(ids));
  }
}
