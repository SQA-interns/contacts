package si.contactgroups.service;

import static org.assertj.core.api.Assertions.assertThat;

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
}
