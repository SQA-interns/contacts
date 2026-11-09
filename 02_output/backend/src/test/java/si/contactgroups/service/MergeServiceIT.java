package si.contactgroups.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import si.contactgroups.domain.AttributePair;
import si.contactgroups.domain.Contact;
import si.contactgroups.domain.ContactData;
import si.contactgroups.domain.HistoryAction;
import si.contactgroups.domain.HistoryEntry;
import si.contactgroups.domain.TrashContactView;
import si.contactgroups.domain.User;

/** US-026: the merge against the real database (R-026-05..09, EX-026-01, EX-026-06..07). */
class MergeServiceIT extends GroupTrashTestSupport {

  @Autowired private ContactService contacts;
  @Autowired private TrashService trash;
  @Autowired private MergeService merges;
  @Autowired private HistoryQueryService history;

  private User ana;
  private UUID acme;

  @BeforeEach
  void anaOwnsAcme() {
    ana = newUser();
    acme = newGroup(ana, "Acme");
  }

  private static ContactData data(String firstName, String phone) {
    return new ContactData(
        firstName, null, "ana@acme.com", phone, null, null, null, null, null, null, null);
  }

  private Contact contact(String firstName, String phone) {
    return contacts.create(acme, ana.id(), data(firstName, phone), null, null);
  }

  private MergeCommand command(
      Contact survivor, List<Contact> all, ContactData fields, AttributePair... attributes) {
    return new MergeCommand(
        survivor.id(),
        all.stream().map(c -> new MergeCommand.Target(c.id(), c.version())).toList(),
        fields,
        List.of(attributes));
  }

  private List<UUID> trashIds() {
    return trash.list(acme).stream().map(TrashContactView::id).toList();
  }

  private List<HistoryEntry> historyOf(Contact contact) {
    return history.contactHistory(acme, contact.id(), 1).items();
  }

  @Test
  void r_026_05_the_survivor_gets_the_chosen_values_and_a_new_version_the_others_go_to_trash() {
    Contact a = contact("Ana", null);
    Contact b = contact("ana", "040 123 456");

    Contact merged =
        merges.merge(acme, ana.id(), command(a, List.of(a, b), data("Ana", "040 123 456")));

    assertThat(merged.id()).isEqualTo(a.id());
    assertThat(merged.version()).isEqualTo(a.version() + 1);
    assertThat(merged.data().phone()).isEqualTo("040 123 456");
    assertThat(trashIds()).containsExactly(b.id());
  }

  @Test
  void r_026_05_the_survivor_takes_the_tags_and_attributes_of_the_merged_contacts() {
    Contact a =
        contacts.create(
            acme,
            ana.id(),
            data("Ana", null),
            List.of(new AttributePair("kompetenca", "java")),
            List.of("udelezenec2025"));
    Contact b =
        contacts.create(
            acme,
            ana.id(),
            data("ana", null),
            List.of(new AttributePair("Kompetenca", "sql"), new AttributePair("jezik", "sl")),
            List.of("neObvescaj"));

    Contact merged =
        merges.merge(
            acme,
            ana.id(),
            command(a, List.of(a, b), data("Ana", null), new AttributePair("kompetenca", "sql")));

    assertThat(merged.tags()).containsExactlyInAnyOrder("udelezenec2025", "neObvescaj");
    assertThat(merged.attributes())
        .containsExactlyInAnyOrder(
            new AttributePair("kompetenca", "sql"), new AttributePair("jezik", "sl"));
  }

  @Test
  void ex_026_01_three_contacts_give_one_survivor_two_in_trash_and_one_merge_entry() {
    Contact a = contact("Ana", null);
    Contact b = contact("Anica", null);
    Contact c = contact("Anja", null);

    merges.merge(acme, ana.id(), command(a, List.of(a, b, c), data("Ana", null)));

    assertThat(trashIds()).containsExactlyInAnyOrder(b.id(), c.id());
    assertThat(historyOf(a).stream().filter(e -> e.action() == HistoryAction.CONTACTS_MERGED))
        .hasSize(1);
  }

  @Test
  void r_026_06_a_result_that_breaks_br_06_merges_nothing() {
    Contact a = contact("Ana", "040 123 456");
    Contact b = contact("Anica", null);
    ContactData onlyPhone =
        new ContactData(null, null, null, "040 123 456", null, null, null, null, null, null, null);

    assertThatThrownBy(() -> merges.merge(acme, ana.id(), command(a, List.of(a, b), onlyPhone)))
        .isInstanceOf(ValidationException.class);

    assertThat(trashIds()).isEmpty();
    assertThat(contacts.get(acme, a.id()).version()).isEqualTo(a.version());
    assertThat(historyOf(a))
        .extracting(HistoryEntry::action)
        .doesNotContain(HistoryAction.CONTACTS_MERGED);
  }

  @Test
  void d_19_d_a_value_no_merged_contact_has_is_refused_and_nothing_changes() {
    Contact a = contact("Ana", null);
    Contact b = contact("Anica", null);

    assertThatThrownBy(
            () -> merges.merge(acme, ana.id(), command(a, List.of(a, b), data("Anja", null))))
        .isInstanceOf(MergeValueException.class);

    assertThat(trashIds()).isEmpty();
  }

  @Test
  void r_026_07_one_entry_lists_the_contacts_and_values_and_the_survivor_shows_the_earlier_ones() {
    Contact a = contact("Ana", null);
    Contact b = contact("Anica", "040 123 456");
    contacts.update(acme, ana.id(), b.id(), b.version(), data("Anica", "041 999 888"), null, null);
    Contact bNow = contacts.get(acme, b.id());

    merges.merge(acme, ana.id(), command(a, List.of(a, bNow), data("Anica", "041 999 888")));

    List<HistoryEntry> entries = historyOf(a);
    HistoryEntry merge =
        entries.stream()
            .filter(e -> e.action() == HistoryAction.CONTACTS_MERGED)
            .findFirst()
            .orElseThrow();
    assertThat(merge.details())
        .contains(a.id().toString(), b.id().toString(), "Anica", "041 999 888");
    assertThat(entries.stream().filter(e -> b.id().equals(e.contactId())).map(HistoryEntry::action))
        .contains(HistoryAction.CONTACT_CREATED, HistoryAction.CONTACT_EDITED);
    assertThat(history.contactHistory(acme, a.id(), 1).total()).isEqualTo(entries.size());
  }

  @Test
  void r_026_07_a_survivor_merged_again_also_shows_the_history_of_the_earlier_merge() {
    Contact a = contact("Ana", null);
    Contact b = contact("Anica", null);
    Contact c = contact("Anja", null);
    merges.merge(acme, ana.id(), command(b, List.of(b, c), data("Anica", null)));
    Contact bNow = contacts.get(acme, b.id());
    Contact aNow = contacts.get(acme, a.id());

    merges.merge(acme, ana.id(), command(aNow, List.of(aNow, bNow), data("Ana", null)));

    assertThat(historyOf(a).stream().map(HistoryEntry::contactId)).contains(a.id(), b.id(), c.id());
    assertThat(historyOf(b).stream().map(HistoryEntry::contactId)).doesNotContain(a.id());
  }

  @Test
  void r_026_08_a_contact_changed_after_the_screen_opened_refuses_the_merge() {
    Contact a = contact("Ana", null);
    Contact b = contact("Anica", null);
    MergeCommand opened = command(a, List.of(a, b), data("Ana", null));
    contacts.update(acme, ana.id(), b.id(), b.version(), data("Anica", "041 1"), null, null);

    assertThatThrownBy(() -> merges.merge(acme, ana.id(), opened))
        .isInstanceOf(ContactChangedException.class);

    assertThat(trashIds()).isEmpty();
    assertThat(contacts.get(acme, a.id()).version()).isEqualTo(a.version());
  }

  @Test
  void ex_026_07_a_contact_in_trash_refuses_the_merge() {
    Contact a = contact("Ana", null);
    Contact b = contact("Anica", null);
    MergeCommand opened = command(a, List.of(a, b), data("Ana", null));
    trash.moveToTrash(acme, ana.id(), b.id());

    assertThatThrownBy(() -> merges.merge(acme, ana.id(), opened))
        .isInstanceOf(ContactInTrashException.class);
    assertThat(contacts.get(acme, a.id()).version()).isEqualTo(a.version());
  }

  @Test
  void
      r_026_09_one_contact_the_same_contact_twice_or_a_survivor_outside_the_selection_is_refused() {
    Contact a = contact("Ana", null);
    Contact b = contact("Anica", null);
    ContactData chosen = data("Ana", null);

    assertThatThrownBy(() -> merges.merge(acme, ana.id(), command(a, List.of(a), chosen)))
        .isInstanceOf(MergeSelectionException.class);
    assertThatThrownBy(() -> merges.merge(acme, ana.id(), command(a, List.of(a, a), chosen)))
        .isInstanceOf(MergeSelectionException.class);
    assertThatThrownBy(() -> merges.merge(acme, ana.id(), command(b, List.of(a, a), chosen)))
        .isInstanceOf(MergeSelectionException.class);
    assertThat(trashIds()).isEmpty();
  }

  @Test
  void r_026_09_a_contact_of_another_group_is_not_found_and_nothing_changes() {
    Contact a = contact("Ana", null);
    UUID beta = newGroup(ana, "Beta");
    Contact other = contacts.create(beta, ana.id(), data("Ana", null), null, null);

    assertThatThrownBy(
            () -> merges.merge(acme, ana.id(), command(a, List.of(a, other), data("Ana", null))))
        .isInstanceOf(ContactNotFoundException.class);
    assertThat(contacts.get(beta, other.id()).version()).isEqualTo(other.version());
  }
}
