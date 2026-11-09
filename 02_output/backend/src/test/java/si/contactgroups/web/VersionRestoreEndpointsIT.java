package si.contactgroups.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.ResultActions;
import si.contactgroups.domain.ContactData;
import si.contactgroups.domain.Role;
import si.contactgroups.domain.User;
import si.contactgroups.service.ContactService;
import si.contactgroups.service.GroupService;
import si.contactgroups.support.WebTestSupport;

/** US-023: restore a Version over the REST interface (contract {@code restoreVersion}). */
class VersionRestoreEndpointsIT extends WebTestSupport {

  @Autowired private JdbcClient jdbc;
  @Autowired private GroupService groups;
  @Autowired private ContactService contacts;

  private User ana;
  private UUID acme;
  private UUID contact;

  @BeforeEach
  void anaOwnsAcmeWithTwoVersions() {
    ana = newUser("Ana Novak");
    acme = groups.createGroup(ana.id(), "Acme").id();
    contact = contacts.create(acme, ana.id(), company("Acme"), null, null).id();
    contacts.update(acme, ana.id(), contact, 1, company("Beta"), null, null);
  }

  private static ContactData company(String company) {
    return new ContactData("Ana", null, null, null, company, null, null, null, null, null, null);
  }

  private ResultActions restore(User user, String number, String body) throws Exception {
    return mvc.perform(
        post("/api/v1/groups/" + acme + "/contacts/" + contact + "/versions/" + number + "/restore")
            .with(as(user))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private void join(User user, Role role) {
    jdbc.sql("INSERT INTO member (group_id, user_id, role) VALUES (:g, :u, :r)")
        .param("g", acme)
        .param("u", user.id())
        .param("r", role.name())
        .update();
  }

  @Test
  void r_023_01_an_owner_restores_and_gets_the_new_contact() throws Exception {
    restore(ana, "1", "{\"version\":2}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(3))
        .andExpect(jsonPath("$.company").value("Acme"));
  }

  @Test
  void r_023_06_a_viewer_is_refused() throws Exception {
    User viola = newUser("Viola Vidmar");
    join(viola, Role.VIEWER);

    restore(viola, "1", "{\"version\":2}")
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("auth.role_forbidden"));
  }

  @Test
  void r_023_04_a_stale_version_is_refused_with_the_current_contact() throws Exception {
    restore(ana, "1", "{\"version\":1}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("contact.changed_by_someone_else"))
        .andExpect(jsonPath("$.current.version").value(2));
  }

  @Test
  void r_023_04_a_request_without_the_version_is_malformed() throws Exception {
    restore(ana, "1", "{}").andExpect(status().isBadRequest());
  }

  @Test
  void r_023_01_a_request_with_an_unknown_property_is_malformed() throws Exception {
    restore(ana, "1", "{\"version\":2,\"company\":\"X\"}").andExpect(status().isBadRequest());
  }

  @Test
  void ex_023_03_a_version_number_that_does_not_exist_is_not_found() throws Exception {
    restore(ana, "9", "{\"version\":2}")
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("version.not_found"));
  }

  @Test
  void ex_023_02_a_contact_in_trash_is_refused() throws Exception {
    jdbc.sql("UPDATE contact SET deleted_at = now() WHERE id = :c").param("c", contact).update();

    restore(ana, "1", "{\"version\":2}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("contact.in_trash"));
  }
}
