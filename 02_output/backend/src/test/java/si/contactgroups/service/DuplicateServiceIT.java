package si.contactgroups.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import si.contactgroups.domain.Contact;
import si.contactgroups.domain.ContactData;
import si.contactgroups.domain.DuplicateSet;
import si.contactgroups.domain.User;

/** US-025: "not a duplicate" stored and applied against the real database. */
class DuplicateServiceIT extends GroupTrashTestSupport {

  @Autowired private ContactService contacts;
  @Autowired private DuplicateService duplicates;

  private Contact novak(UUID group, User user, String city) {
    return contacts.create(
        group,
        user.id(),
        new ContactData("Ana", "Novak", null, null, "Acme", city, null, null, null, null, null),
        null,
        null);
  }

  @Test
  void r_025_01_a_dismissed_set_is_stored_once_with_its_contacts_and_no_longer_found() {
    User ana = newUser();
    UUID acme = newGroup(ana, "Acme");
    Contact a = novak(acme, ana, "Kranj");
    Contact b = novak(acme, ana, "Celje");

    duplicates.dismiss(acme, ana.id(), List.of(a.id(), b.id()));
    duplicates.dismiss(acme, ana.id(), List.of(b.id(), a.id()));

    assertThat(duplicates.find(acme)).isEmpty();
    assertThat(count("duplicate_dismissal", acme)).isEqualTo(1);
    assertThat(
            jdbc.sql(
                    "SELECT count(*) FROM duplicate_dismissal_contact dc"
                        + " JOIN duplicate_dismissal d ON d.id = dc.dismissal_id"
                        + " WHERE d.group_id = :g")
                .param("g", acme)
                .query(Integer.class)
                .single())
        .isEqualTo(2);
  }

  @Test
  void r_025_03_a_new_matching_contact_makes_the_bigger_set_visible() {
    User ana = newUser();
    UUID acme = newGroup(ana, "Acme");
    Contact a = novak(acme, ana, "Kranj");
    Contact b = novak(acme, ana, "Celje");
    duplicates.dismiss(acme, ana.id(), List.of(a.id(), b.id()));

    Contact c = novak(acme, ana, "Koper");

    List<DuplicateSet> sets = duplicates.find(acme);
    assertThat(sets).hasSize(1);
    assertThat(sets.get(0).contacts()).hasSize(3);
    assertThat(sets.get(0).contacts()).anyMatch(item -> item.id().equals(c.id()));
  }

  @Test
  void ex_025_03_ids_that_are_no_set_of_the_group_are_not_found() {
    User ana = newUser();
    UUID acme = newGroup(ana, "Acme");
    UUID beta = newGroup(ana, "Beta");
    Contact a = novak(beta, ana, "Kranj");
    Contact b = novak(beta, ana, "Celje");
    Contact single = novak(acme, ana, "Koper");

    assertThatThrownBy(() -> duplicates.dismiss(acme, ana.id(), List.of(a.id(), b.id())))
        .isInstanceOf(DuplicateSetNotFoundException.class);
    assertThatThrownBy(() -> duplicates.dismiss(acme, ana.id(), List.of(single.id())))
        .isInstanceOf(DuplicateSetNotFoundException.class);
    assertThatThrownBy(() -> duplicates.dismiss(acme, ana.id(), null))
        .isInstanceOf(DuplicateSetNotFoundException.class);
    assertThat(count("duplicate_dismissal", acme)).isZero();
  }

  @Test
  void r_025_05_dismissing_adds_no_version_and_no_history() {
    User ana = newUser();
    UUID acme = newGroup(ana, "Acme");
    Contact a = novak(acme, ana, "Kranj");
    Contact b = novak(acme, ana, "Celje");
    int versions = versionCount(acme);
    int history = count("history_entry", acme);

    duplicates.dismiss(acme, ana.id(), List.of(a.id(), b.id()));

    assertThat(versionCount(acme)).isEqualTo(versions);
    assertThat(count("history_entry", acme)).isEqualTo(history);
  }

  private int versionCount(UUID group) {
    return jdbc.sql(
            "SELECT count(*) FROM contact_version v JOIN contact c ON c.id = v.contact_id"
                + " WHERE c.group_id = :g")
        .param("g", group)
        .query(Integer.class)
        .single();
  }
}
