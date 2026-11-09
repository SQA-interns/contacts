package si.contactgroups.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** R-016-02: the old and new value of every changed field, nothing for unchanged ones. */
class ContactChangesTest {

  private static ContactData ana(String lastName, String company) {
    return new ContactData(
        "Ana", lastName, null, null, company, null, null, null, null, null, null);
  }

  @Test
  void ac_016_02_only_the_changed_field_is_listed() {
    var changes = ContactChanges.between(ana("Novak", "Acme"), ana("Kos", "Acme"));

    assertThat(changes).containsOnlyKeys("lastName");
    assertThat(changes.get("lastName")).isEqualTo(new ContactChanges.Change("Novak", "Kos"));
  }

  @Test
  void ex_016_02_empty_to_value_keeps_the_empty_old_value() {
    var changes = ContactChanges.between(ana("Novak", null), ana("Novak", "Acme"));

    assertThat(changes.get("company")).isEqualTo(new ContactChanges.Change(null, "Acme"));
  }

  @Test
  void r_016_01_a_new_contact_lists_its_stored_values_only() {
    var values = ContactChanges.stored(ana("Novak", null));

    assertThat(values).containsOnlyKeys("firstName", "lastName");
  }

  @Test
  void ac_016_08_equal_data_gives_no_change() {
    assertThat(ContactChanges.between(ana("Novak", "Acme"), ana("Novak", "Acme"))).isEmpty();
  }

  @Test
  void ex_016_03_five_changed_fields_give_five_pairs() {
    var before = ana("Novak", "Acme");
    var after =
        new ContactData(
            "Ana",
            "Novak",
            null,
            "+38640555666",
            "Acme",
            "Vodja",
            "Ulica 1",
            "2000",
            "Maribor",
            null,
            null);

    assertThat(ContactChanges.between(before, after))
        .containsOnlyKeys("phone", "jobTitle", "street", "postalCode", "city");
  }
}
