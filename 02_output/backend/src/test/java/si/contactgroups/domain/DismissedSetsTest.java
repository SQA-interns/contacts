package si.contactgroups.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** R-025-02, R-025-03, EX-025-01, EX-025-02: which Duplicate sets a dismissal hides. */
class DismissedSetsTest {

  private static final UUID A = UUID.randomUUID();
  private static final UUID B = UUID.randomUUID();
  private static final UUID C = UUID.randomUUID();
  private static final UUID D = UUID.randomUUID();

  private static DuplicateSet set(UUID... ids) {
    List<ContactListItem> contacts =
        java.util.Arrays.stream(ids)
            .map(
                id ->
                    new ContactListItem(
                        id, "Ana", "Novak", null, "Acme", null, null, List.of(), 1, Instant.EPOCH))
            .toList();
    return new DuplicateSet(
        DuplicateFinder.keyOf(java.util.Arrays.asList(ids)),
        DuplicateMark.PROBABLE,
        "duplicate.same_name_company",
        java.util.Map.of(),
        contacts);
  }

  @Test
  void r_025_02_a_set_of_exactly_the_dismissed_contacts_is_hidden() {
    List<DuplicateSet> shown = DismissedSets.hide(List.of(set(A, B)), List.of(Set.of(A, B)));

    assertThat(shown).isEmpty();
  }

  @Test
  void r_025_03_a_set_with_one_more_contact_is_shown() {
    List<DuplicateSet> shown = DismissedSets.hide(List.of(set(A, B, C)), List.of(Set.of(A, B)));

    assertThat(shown).hasSize(1);
  }

  @Test
  void ex_025_02_the_rest_of_a_dismissed_set_is_hidden() {
    List<DuplicateSet> shown = DismissedSets.hide(List.of(set(A, B)), List.of(Set.of(A, B, C)));

    assertThat(shown).isEmpty();
  }

  @Test
  void ex_025_01_a_set_that_mixes_dismissed_and_new_contacts_is_shown() {
    List<DuplicateSet> shown = DismissedSets.hide(List.of(set(A, B, D)), List.of(Set.of(A, B, C)));

    assertThat(shown).hasSize(1);
  }

  @Test
  void a_set_spread_over_two_dismissals_is_shown() {
    List<DuplicateSet> shown =
        DismissedSets.hide(List.of(set(A, B, C)), List.of(Set.of(A, B), Set.of(B, C)));

    assertThat(shown).hasSize(1);
  }

  @Test
  void without_dismissals_every_set_is_shown_in_order() {
    List<DuplicateSet> sets = List.of(set(A, B), set(C, D));

    assertThat(DismissedSets.hide(sets, List.of())).isEqualTo(sets);
  }
}
