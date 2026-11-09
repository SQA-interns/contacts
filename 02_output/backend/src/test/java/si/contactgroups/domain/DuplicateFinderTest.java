package si.contactgroups.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** R-024-02..05, R-024-08, R-024-10, EX-024-01..04, EX-024-06: how Duplicate sets are built. */
class DuplicateFinderTest {

  private static int created;

  private static DuplicateCandidate contact(
      String first, String last, String email, String company, String phone) {
    ContactListItem item =
        new ContactListItem(
            UUID.randomUUID(),
            first,
            last,
            email,
            company,
            null,
            null,
            List.of(),
            1,
            Instant.EPOCH.plusSeconds(created++));
    return new DuplicateCandidate(item, phone);
  }

  private static List<UUID> ids(DuplicateSet set) {
    return set.contacts().stream().map(ContactListItem::id).toList();
  }

  private static UUID id(DuplicateCandidate candidate) {
    return candidate.item().id();
  }

  @Test
  void r_024_02_the_same_email_ignoring_case_is_one_certain_set_with_its_reason() {
    var a = contact("Ana", null, "ana@acme.com", null, null);
    var b = contact("Anica", null, "ANA@acme.com", null, null);
    var c = contact("Anja", null, "Ana@Acme.com", null, null);

    List<DuplicateSet> sets = DuplicateFinder.find(List.of(a, b, c));

    assertThat(sets).hasSize(1);
    assertThat(sets.get(0).mark()).isEqualTo(DuplicateMark.CERTAIN);
    assertThat(ids(sets.get(0))).containsExactly(id(a), id(b), id(c));
    assertThat(sets.get(0).reasonCode()).isEqualTo("duplicate.same_email");
    assertThat(sets.get(0).reasonParams()).containsEntry("email", "ana@acme.com");
    assertThat(sets.get(0).reasonParams()).containsEntry("count", "3");
  }

  @Test
  void ex_024_03_contacts_without_an_email_are_not_certain_duplicates() {
    var a = contact("Ana", "Novak", null, null, null);
    var b = contact("Boris", "Kos", "", null, null);

    assertThat(DuplicateFinder.find(List.of(a, b))).isEmpty();
  }

  @Test
  void r_024_03_the_same_name_and_company_ignoring_case_and_accents_is_one_probable_set() {
    var a = contact("Ana", "Novak", null, "Acme", null);
    var b = contact("ana", "novak", "a@x.si", "ACME", null);

    List<DuplicateSet> sets = DuplicateFinder.find(List.of(a, b));

    assertThat(sets).hasSize(1);
    assertThat(sets.get(0).mark()).isEqualTo(DuplicateMark.PROBABLE);
    assertThat(ids(sets.get(0))).containsExactly(id(a), id(b));
    assertThat(sets.get(0).reasonCode()).isEqualTo("duplicate.same_name_company");
    assertThat(sets.get(0).reasonParams()).containsEntry("count", "2");
  }

  @Test
  void r_024_03_the_same_name_and_phone_without_spaces_and_separators_is_one_probable_set() {
    var a = contact("Ana", "Novak", null, "Acme", "040 123 456");
    var b = contact("Ana", "Novak", null, "Beta", "040-123-456");
    var c = contact("Ana", "Novak", null, "Gama", "(040) 123.456");

    List<DuplicateSet> sets = DuplicateFinder.find(List.of(a, b, c));

    assertThat(sets).hasSize(1);
    assertThat(ids(sets.get(0))).containsExactly(id(a), id(b), id(c));
    assertThat(sets.get(0).reasonCode()).isEqualTo("duplicate.same_name_phone");
  }

  @Test
  void r_024_03_the_same_name_with_another_company_and_phone_is_no_duplicate() {
    var a = contact("Ana", "Novak", null, "Acme", "040 111 111");
    var b = contact("Ana", "Novak", null, "Beta", "040 222 222");

    assertThat(DuplicateFinder.find(List.of(a, b))).isEmpty();
  }

  @Test
  void ex_024_01_the_names_skerlj_with_and_without_the_caron_are_equal() {
    var a = contact("Ana", "Škerlj", null, "Acme", null);
    var b = contact("Ana", "Skerlj", null, "Acme", null);

    assertThat(DuplicateFinder.find(List.of(a, b))).hasSize(1);
  }

  @Test
  void ex_024_02_the_same_name_with_no_company_and_no_phone_is_no_duplicate() {
    var a = contact("Ana", "Novak", null, null, null);
    var b = contact("Ana", "Novak", null, "", "");

    assertThat(DuplicateFinder.find(List.of(a, b))).isEmpty();
  }

  @Test
  void ex_024_04_the_same_first_name_and_company_with_another_last_name_is_no_duplicate() {
    var a = contact("Ana", "Novak", null, "Acme", null);
    var b = contact("Ana", "Kos", null, "Acme", null);

    assertThat(DuplicateFinder.find(List.of(a, b))).isEmpty();
  }

  @Test
  void ex_024_06_a_first_name_without_a_last_name_is_not_matched() {
    var a = contact("Ana", null, null, "Acme", null);
    var b = contact("Ana", null, null, "Acme", null);

    assertThat(DuplicateFinder.find(List.of(a, b))).isEmpty();
  }

  @Test
  void r_024_04_a_contact_with_only_a_phone_and_notes_is_in_no_set() {
    var a = contact(null, null, null, null, "040 123 456");
    var b = contact(null, null, null, null, "040 123 456");

    assertThat(DuplicateFinder.find(List.of(a, b))).isEmpty();
  }

  @Test
  void r_024_08_a_contact_of_a_certain_set_is_shown_only_in_that_set() {
    var a = contact("Ana", "Novak", "ana@acme.com", "Acme", null);
    var b = contact("Anica", "Kos", "ana@acme.com", null, null);
    var c = contact("Ana", "Novak", null, "Acme", null);

    List<DuplicateSet> sets = DuplicateFinder.find(List.of(a, b, c));

    assertThat(sets).hasSize(1);
    assertThat(sets.get(0).mark()).isEqualTo(DuplicateMark.CERTAIN);
    assertThat(ids(sets.get(0))).containsExactly(id(a), id(b));
  }

  @Test
  void d_19c_contacts_linked_through_company_and_phone_form_one_set() {
    var a = contact("Ana", "Novak", null, "Acme", null);
    var b = contact("Ana", "Novak", null, "Acme", "040 123 456");
    var c = contact("Ana", "Novak", null, "Beta", "040123456");

    List<DuplicateSet> sets = DuplicateFinder.find(List.of(a, b, c));

    assertThat(sets).hasSize(1);
    assertThat(ids(sets.get(0))).containsExactly(id(a), id(b), id(c));
    assertThat(sets.get(0).reasonCode()).isEqualTo("duplicate.same_name_company_or_phone");
  }

  @Test
  void r_024_05_a_set_has_a_stable_key_of_its_sorted_contact_ids() {
    var a = contact("Ana", null, "ana@acme.com", null, null);
    var b = contact("Anica", null, "ana@acme.com", null, null);

    String forward = DuplicateFinder.find(List.of(a, b)).get(0).key();
    String backward = DuplicateFinder.find(List.of(b, a)).get(0).key();

    assertThat(forward).hasSize(64).isEqualTo(backward);
  }

  @Test
  void r_024_10_no_contacts_or_no_duplicates_give_no_sets() {
    assertThat(DuplicateFinder.find(List.of())).isEmpty();
    assertThat(DuplicateFinder.find(List.of(contact("Ana", "Novak", "ana@acme.com", "Acme", null))))
        .isEmpty();
  }

  @Test
  void r_024_01_certain_sets_come_before_probable_sets() {
    var p1 = contact("Boris", "Kos", null, "Beta", null);
    var p2 = contact("Boris", "Kos", null, "Beta", null);
    var c1 = contact("Ana", null, "ana@acme.com", null, null);
    var c2 = contact("Anica", null, "ana@acme.com", null, null);

    List<DuplicateSet> sets = DuplicateFinder.find(List.of(p1, p2, c1, c2));

    assertThat(sets)
        .extracting(DuplicateSet::mark)
        .containsExactly(DuplicateMark.CERTAIN, DuplicateMark.PROBABLE);
  }
}
