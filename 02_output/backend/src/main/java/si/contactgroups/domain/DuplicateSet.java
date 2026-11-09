package si.contactgroups.domain;

import java.util.List;
import java.util.Map;

/**
 * Contacts that probably describe one person (contract schema {@code DuplicateSet}). It is computed
 * on every search and never stored; {@code key} is the hash of the sorted Contact ids.
 */
public record DuplicateSet(
    String key,
    DuplicateMark mark,
    String reasonCode,
    Map<String, String> reasonParams,
    List<ContactListItem> contacts) {}
