package si.contactgroups.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
}
