package si.contactgroups.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds the Duplicate sets of one Group (US-024). Certain: the same e-mail address ignoring case.
 * Probable: the same first and last name (BR-05) and the same company or the same phone number;
 * Contacts linked through company or phone form one set (D-19c). A Contact of a certain set is in
 * no other set (R-024-08). The search reads only; it changes nothing (R-024-06).
 */
public final class DuplicateFinder {

  private static final Comparator<ContactListItem> OLDEST_FIRST =
      Comparator.comparing(ContactListItem::createdAt).thenComparing(ContactListItem::id);

  private DuplicateFinder() {}

  /** The sets among the candidates: certain ones first, then probable ones, each oldest first. */
  public static List<DuplicateSet> find(List<DuplicateCandidate> candidates) {
    List<DuplicateSet> certain = certainSets(candidates);
    Set<UUID> inCertain = new HashSet<>();
    certain.forEach(set -> set.contacts().forEach(c -> inCertain.add(c.id())));
    List<DuplicateCandidate> rest =
        candidates.stream().filter(c -> !inCertain.contains(c.item().id())).toList();
    List<DuplicateSet> all = new ArrayList<>(ordered(certain));
    all.addAll(ordered(probableSets(rest)));
    return all;
  }

  private static List<DuplicateSet> ordered(List<DuplicateSet> sets) {
    return sets.stream()
        .sorted(Comparator.comparing(set -> set.contacts().get(0), OLDEST_FIRST))
        .toList();
  }

  private static List<DuplicateSet> certainSets(List<DuplicateCandidate> candidates) {
    return groups(candidates, DuplicateFinder::emailOf).values().stream()
        .filter(group -> group.size() > 1)
        .map(DuplicateFinder::certainSet)
        .toList();
  }

  private static DuplicateSet certainSet(List<DuplicateCandidate> group) {
    String email = emailOf(group.get(0));
    return set(
        DuplicateMark.CERTAIN,
        "duplicate.same_email",
        Map.of("email", email, "count", String.valueOf(group.size())),
        group);
  }

  private static List<DuplicateSet> probableSets(List<DuplicateCandidate> candidates) {
    List<DuplicateSet> sets = new ArrayList<>();
    for (List<DuplicateCandidate> sameName : groups(candidates, DuplicateFinder::nameOf).values()) {
      if (sameName.size() > 1) {
        linked(sameName).stream()
            .filter(group -> group.size() > 1)
            .forEach(g -> sets.add(probableSet(g)));
      }
    }
    return sets;
  }

  /** The Contacts of one name split into the groups that share a company or a phone number. */
  private static List<List<DuplicateCandidate>> linked(List<DuplicateCandidate> sameName) {
    int[] parent = new int[sameName.size()];
    for (int i = 0; i < parent.length; i++) {
      parent[i] = i;
    }
    union(sameName, parent, DuplicateFinder::companyOf);
    union(sameName, parent, DuplicateFinder::phoneOf);
    Map<Integer, List<DuplicateCandidate>> byRoot = new LinkedHashMap<>();
    for (int i = 0; i < parent.length; i++) {
      byRoot.computeIfAbsent(root(parent, i), k -> new ArrayList<>()).add(sameName.get(i));
    }
    return List.copyOf(byRoot.values());
  }

  private static void union(
      List<DuplicateCandidate> sameName, int[] parent, Function<DuplicateCandidate, String> key) {
    Map<String, Integer> first = new LinkedHashMap<>();
    for (int i = 0; i < sameName.size(); i++) {
      String value = key.apply(sameName.get(i));
      if (!value.isEmpty()) {
        Integer seen = first.putIfAbsent(value, i);
        if (seen != null) {
          parent[root(parent, i)] = root(parent, seen);
        }
      }
    }
  }

  private static int root(int[] parent, int index) {
    int current = index;
    while (parent[current] != current) {
      current = parent[current];
    }
    return current;
  }

  private static DuplicateSet probableSet(List<DuplicateCandidate> group) {
    boolean company = sharedByTwo(group, DuplicateFinder::companyOf);
    boolean phone = sharedByTwo(group, DuplicateFinder::phoneOf);
    String code =
        "duplicate.same_name_"
            + (company && phone ? "company_or_phone" : phone ? "phone" : "company");
    ContactListItem first = group.get(0).item();
    return set(
        DuplicateMark.PROBABLE,
        code,
        Map.of(
            "name", (first.firstName() + " " + first.lastName()).strip(),
            "count", String.valueOf(group.size())),
        group);
  }

  private static boolean sharedByTwo(
      List<DuplicateCandidate> group, Function<DuplicateCandidate, String> key) {
    return groups(group, key).values().stream().anyMatch(same -> same.size() > 1);
  }

  private static DuplicateSet set(
      DuplicateMark mark, String code, Map<String, String> params, List<DuplicateCandidate> group) {
    List<ContactListItem> contacts =
        group.stream().map(DuplicateCandidate::item).sorted(OLDEST_FIRST).toList();
    return new DuplicateSet(
        keyOf(contacts.stream().map(ContactListItem::id).toList()), mark, code, params, contacts);
  }

  /** Groups by a key, leaving out the Contacts whose key is empty. */
  private static Map<String, List<DuplicateCandidate>> groups(
      List<DuplicateCandidate> candidates, Function<DuplicateCandidate, String> key) {
    return candidates.stream()
        .filter(c -> !key.apply(c).isEmpty())
        .collect(Collectors.groupingBy(key, LinkedHashMap::new, Collectors.toList()));
  }

  private static String emailOf(DuplicateCandidate c) {
    return blankToEmpty(c.item().email()).toLowerCase(Locale.ROOT);
  }

  /** First and last name, both required (EX-024-06), compared by BR-05. */
  private static String nameOf(DuplicateCandidate c) {
    String first = normalised(c.item().firstName());
    String last = normalised(c.item().lastName());
    return first.isEmpty() || last.isEmpty() ? "" : first + "\n" + last;
  }

  private static String companyOf(DuplicateCandidate c) {
    return normalised(c.item().company());
  }

  /** The phone number without spaces and {@code - ( ) . /} (the story's definition). */
  private static String phoneOf(DuplicateCandidate c) {
    return blankToEmpty(c.phone()).replaceAll("[\\s\\-().\\/]", "");
  }

  private static String normalised(String text) {
    return blankToEmpty(Normaliser.normalise(text));
  }

  private static String blankToEmpty(String text) {
    return text == null ? "" : text.strip();
  }

  /** The key of a set: the SHA-256 (hex) of the sorted Contact ids (contract {@code key}). */
  public static String keyOf(Collection<UUID> contactIds) {
    String ids = contactIds.stream().map(UUID::toString).sorted().collect(Collectors.joining(","));
    try {
      byte[] hash =
          MessageDigest.getInstance("SHA-256").digest(ids.getBytes(StandardCharsets.UTF_8));
      return java.util.HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is part of every Java platform", e);
    }
  }
}
