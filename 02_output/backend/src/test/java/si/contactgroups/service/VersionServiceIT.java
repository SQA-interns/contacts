package si.contactgroups.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import si.contactgroups.domain.AttributePair;
import si.contactgroups.domain.Contact;
import si.contactgroups.domain.ContactData;
import si.contactgroups.domain.User;
import si.contactgroups.domain.VersionSummary;

/** US-022: reading Versions against the real database. */
class VersionServiceIT extends GroupTrashTestSupport {

  @Autowired private ContactService contacts;
  @Autowired private VersionService versions;

  private static ContactData named(String firstName) {
    return new ContactData(
        firstName, "Novak", null, null, null, null, null, null, null, null, null);
  }

  @Test
  void a_version_keeps_the_name_of_a_user_whose_account_is_gone() {
    User ana = newUser();
    UUID acme = newGroup(ana, "Acme");
    Contact created = contacts.create(acme, ana.id(), named("Ana"), null, null);
    jdbc.sql(
            "INSERT INTO history_entry (group_id, user_id, user_name, action, contact_id)"
                + " VALUES (:g, :u, 'Ana Novak', 'CONTACT_CREATED', :c)")
        .param("g", acme)
        .param("u", ana.id())
        .param("c", created.id())
        .update();
    jdbc.sql("DELETE FROM member WHERE user_id = :u").param("u", ana.id()).update();
    jdbc.sql("DELETE FROM app_user WHERE id = :u").param("u", ana.id()).update();

    List<VersionSummary> list = versions.list(acme, created.id());

    assertThat(list).extracting(VersionSummary::userName).containsExactly("Ana Novak");
  }

  @Test
  void a_version_of_another_group_is_not_found() {
    User ana = newUser();
    UUID acme = newGroup(ana, "Acme");
    UUID beta = newGroup(ana, "Beta");
    Contact created = contacts.create(acme, ana.id(), named("Ana"), null, null);

    assertThatThrownBy(() -> versions.get(beta, created.id(), "1"))
        .isInstanceOf(ContactNotFoundException.class);
  }

  @Test
  void a_number_that_is_not_a_number_is_not_found() {
    User ana = newUser();
    UUID acme = newGroup(ana, "Acme");
    Contact created = contacts.create(acme, ana.id(), named("Ana"), null, null);

    assertThatThrownBy(() -> versions.get(acme, created.id(), "x"))
        .isInstanceOf(VersionNotFoundException.class);
  }

  private static ContactData company(String company) {
    return new ContactData("Ana", "Novak", null, null, company, null, null, null, null, null, null);
  }

  private Contact threeCompanies(User ana, UUID acme) {
    Contact v1 = contacts.create(acme, ana.id(), company("Acme"), null, null);
    contacts.update(acme, ana.id(), v1.id(), 1, company("Beta"), null, null);
    contacts.update(acme, ana.id(), v1.id(), 2, company("Cezar"), null, null);
    return v1;
  }

  private List<String> historyActions(UUID acme, UUID contactId) {
    return jdbc.sql(
            "SELECT action FROM history_entry WHERE group_id = :g AND contact_id = :c"
                + " ORDER BY occurred_at DESC, id DESC")
        .param("g", acme)
        .param("c", contactId)
        .query(String.class)
        .list();
  }

  @Test
  void r_023_01_the_content_of_an_earlier_version_is_saved_as_a_new_version() {
    User ana = newUser();
    UUID acme = newGroup(ana, "Acme");
    Contact c = threeCompanies(ana, acme);

    Contact restored = versions.restore(acme, ana.id(), c.id(), "1", 3);

    assertThat(restored.version()).isEqualTo(4);
    assertThat(restored.data().company()).isEqualTo("Acme");
    assertThat(versions.list(acme, c.id()))
        .extracting(VersionSummary::number)
        .containsExactly(4, 3, 2, 1);
    assertThat(versions.get(acme, c.id(), "3").content().data().company()).isEqualTo("Cezar");
  }

  @Test
  void r_023_02_the_history_entry_names_the_restored_version_and_the_changed_fields() {
    User ana = newUser();
    UUID acme = newGroup(ana, "Acme");
    Contact c = threeCompanies(ana, acme);

    versions.restore(acme, ana.id(), c.id(), "1", 3);

    assertThat(historyActions(acme, c.id())).first().isEqualTo("VERSION_RESTORED");
    String details =
        jdbc.sql(
                "SELECT CAST(details AS text) FROM history_entry"
                    + " WHERE contact_id = :c AND action = 'VERSION_RESTORED'")
            .param("c", c.id())
            .query(String.class)
            .single();
    assertThat(details)
        .contains("\"restored\": 1")
        .contains("\"company\"")
        .contains("Cezar")
        .contains("Acme");
  }

  @Test
  void r_023_01_a_restore_makes_no_edit_entry_besides_the_restore_entry() {
    User ana = newUser();
    UUID acme = newGroup(ana, "Acme");
    Contact c = threeCompanies(ana, acme);

    versions.restore(acme, ana.id(), c.id(), "1", 3);

    assertThat(historyActions(acme, c.id()))
        .containsExactly("VERSION_RESTORED", "CONTACT_EDITED", "CONTACT_EDITED", "CONTACT_CREATED");
  }

  @Test
  void r_023_04_a_version_number_that_is_not_the_current_one_is_refused() {
    User ana = newUser();
    UUID acme = newGroup(ana, "Acme");
    Contact c = threeCompanies(ana, acme);

    assertThatThrownBy(() -> versions.restore(acme, ana.id(), c.id(), "1", 2))
        .isInstanceOf(ContactChangedException.class);
    assertThat(versions.list(acme, c.id())).hasSize(3);
  }

  @Test
  void r_023_01_restoring_the_current_version_makes_no_new_version() {
    User ana = newUser();
    UUID acme = newGroup(ana, "Acme");
    Contact c = threeCompanies(ana, acme);

    Contact same = versions.restore(acme, ana.id(), c.id(), "3", 3);

    assertThat(same.version()).isEqualTo(3);
    assertThat(versions.list(acme, c.id())).hasSize(3);
    assertThat(historyActions(acme, c.id())).doesNotContain("VERSION_RESTORED");
  }

  @Test
  void r_023_01_a_version_number_that_does_not_exist_is_not_found() {
    User ana = newUser();
    UUID acme = newGroup(ana, "Acme");
    Contact c = threeCompanies(ana, acme);

    assertThatThrownBy(() -> versions.restore(acme, ana.id(), c.id(), "9", 3))
        .isInstanceOf(VersionNotFoundException.class);
    assertThatThrownBy(() -> versions.restore(acme, ana.id(), c.id(), "x", 3))
        .isInstanceOf(VersionNotFoundException.class);
  }

  @Test
  void r_023_04_a_missing_version_number_in_the_request_is_refused() {
    User ana = newUser();
    UUID acme = newGroup(ana, "Acme");
    Contact c = threeCompanies(ana, acme);

    assertThatThrownBy(() -> versions.restore(acme, ana.id(), c.id(), "1", null))
        .isInstanceOf(VersionMissingException.class);
  }

  @Test
  void r_023_01_a_contact_in_trash_is_not_restored_from() {
    User ana = newUser();
    UUID acme = newGroup(ana, "Acme");
    Contact c = threeCompanies(ana, acme);
    jdbc.sql("UPDATE contact SET deleted_at = now() WHERE id = :c").param("c", c.id()).update();

    assertThatThrownBy(() -> versions.restore(acme, ana.id(), c.id(), "1", 3))
        .isInstanceOf(ContactInTrashException.class);
  }

  @Test
  void r_023_03_content_that_breaks_br_06_restores_nothing() {
    User ana = newUser();
    UUID acme = newGroup(ana, "Acme");
    Contact c = contacts.create(acme, ana.id(), email("ana@x.si"), null, null);
    contacts.update(acme, ana.id(), c.id(), 1, email("ana@y.si"), null, null);
    jdbc.sql(
            "UPDATE contact_version SET snapshot = jsonb_set(snapshot, '{email}', '\"ana@\"')"
                + " WHERE contact_id = :c AND number = 1")
        .param("c", c.id())
        .update();

    assertThatThrownBy(() -> versions.restore(acme, ana.id(), c.id(), "1", 2))
        .isInstanceOf(ValidationException.class);
    assertThat(versions.list(acme, c.id())).hasSize(2);
    assertThat(contacts.get(acme, c.id()).data().email()).isEqualTo("ana@y.si");
  }

  private static ContactData email(String email) {
    return new ContactData("Ana", null, email, null, null, null, null, null, null, null, null);
  }

  @Test
  void r_023_05_a_tag_deleted_since_is_created_again_and_put_on_the_contact() {
    User ana = newUser();
    UUID acme = newGroup(ana, "Acme");
    Contact c = contacts.create(acme, ana.id(), company("Acme"), null, List.of("gost"));
    contacts.update(acme, ana.id(), c.id(), 1, company("Beta"), null, List.of());
    jdbc.sql("DELETE FROM tag WHERE group_id = :g AND name = 'gost'").param("g", acme).update();

    Contact restored = versions.restore(acme, ana.id(), c.id(), "1", 2);

    assertThat(restored.tags()).containsExactly("gost");
    assertThat(
            jdbc.sql("SELECT count(*) FROM tag WHERE group_id = :g AND name = 'gost'")
                .param("g", acme)
                .query(Integer.class)
                .single())
        .isEqualTo(1);
  }

  @Test
  void r_023_01_the_attributes_of_the_version_come_back() {
    User ana = newUser();
    UUID acme = newGroup(ana, "Acme");
    Contact c =
        contacts.create(
            acme,
            ana.id(),
            company("Acme"),
            List.of(new AttributePair("Rojstni dan", "1.1.")),
            null);
    contacts.update(acme, ana.id(), c.id(), 1, company("Acme"), List.of(), null);

    Contact restored = versions.restore(acme, ana.id(), c.id(), "1", 2);

    assertThat(restored.attributes()).containsExactly(new AttributePair("Rojstni dan", "1.1."));
  }
}
