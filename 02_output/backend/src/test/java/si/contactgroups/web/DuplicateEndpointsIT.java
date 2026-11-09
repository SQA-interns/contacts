package si.contactgroups.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import si.contactgroups.service.TrashService;
import si.contactgroups.support.WebTestSupport;

/** US-024, US-025: the duplicate search and "not a duplicate" over the REST interface. */
class DuplicateEndpointsIT extends WebTestSupport {

  @Autowired private JdbcClient jdbc;
  @Autowired private GroupService groups;
  @Autowired private ContactService contacts;
  @Autowired private TrashService trash;

  private User ana;
  private UUID acme;

  @BeforeEach
  void anaOwnsAcme() {
    ana = newUser("Ana Novak");
    acme = groups.createGroup(ana.id(), "Acme").id();
  }

  private UUID contact(UUID group, String firstName, String email, String phone) {
    ContactData data =
        new ContactData(firstName, null, email, phone, null, null, null, null, null, null, null);
    return contacts.create(group, ana.id(), data, null, null).id();
  }

  private ResultActions search(User user) throws Exception {
    return mvc.perform(get("/api/v1/groups/" + acme + "/duplicates").with(as(user)));
  }

  @Test
  void r_024_01_an_owner_gets_the_sets_with_mark_reason_and_contacts() throws Exception {
    contact(acme, "Ana", "ana@acme.com", null);
    contact(acme, "Anica", "ANA@acme.com", null);

    search(ana)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].mark").value("certain"))
        .andExpect(jsonPath("$[0].reasonCode").value("duplicate.same_email"))
        .andExpect(jsonPath("$[0].reasonParams.email").value("ana@acme.com"))
        .andExpect(jsonPath("$[0].reasonParams.count").value("2"))
        .andExpect(jsonPath("$[0].key").isNotEmpty())
        .andExpect(jsonPath("$[0].contacts.length()").value(2))
        .andExpect(jsonPath("$[0].contacts[0].firstName").value("Ana"));
  }

  @Test
  void r_024_01_a_contact_in_trash_is_in_no_set() throws Exception {
    contact(acme, "Ana", "ana@acme.com", null);
    UUID gone = contact(acme, "Anica", "ana@acme.com", null);
    trash.moveToTrash(acme, ana.id(), gone);

    search(ana).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void r_024_07_contacts_of_another_group_are_not_compared() throws Exception {
    UUID beta = groups.createGroup(ana.id(), "Beta").id();
    contact(acme, "Ana", "ana@acme.com", null);
    contact(beta, "Ana", "ana@acme.com", null);

    search(ana).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void r_024_09_a_viewer_is_refused() throws Exception {
    User viola = newUser("Viola Vidmar");
    jdbc.sql("INSERT INTO member (group_id, user_id, role) VALUES (:g, :u, :r)")
        .param("g", acme)
        .param("u", viola.id())
        .param("r", Role.VIEWER.name())
        .update();

    search(viola)
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("auth.role_forbidden"));
  }

  @Test
  void r_024_03_the_phone_of_a_contact_is_compared_without_separators() throws Exception {
    ContactData first =
        new ContactData(
            "Ana", "Novak", null, "040 123 456", null, null, null, null, null, null, null);
    ContactData second =
        new ContactData(
            "Ana", "Novak", null, "040-123-456", null, null, null, null, null, null, null);
    contacts.create(acme, ana.id(), first, null, null);
    contacts.create(acme, ana.id(), second, null, null);

    search(ana)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].mark").value("probable"))
        .andExpect(jsonPath("$[0].reasonCode").value("duplicate.same_name_phone"));
  }

  private ResultActions dismiss(User user, String body) throws Exception {
    return mvc.perform(
        post("/api/v1/groups/" + acme + "/duplicates/dismissals")
            .with(as(user))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  @Test
  void r_025_01_a_set_is_dismissed_with_204_and_is_gone_from_the_search() throws Exception {
    UUID a = contact(acme, "Ana", "ana@acme.com", null);
    UUID b = contact(acme, "Anica", "ana@acme.com", null);

    dismiss(ana, "{\"contactIds\":[\"" + a + "\",\"" + b + "\"]}")
        .andExpect(status().isNoContent());

    search(ana).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void ex_025_03_ids_that_are_no_set_answer_404_duplicate_not_found() throws Exception {
    dismiss(ana, "{\"contactIds\":[\"" + UUID.randomUUID() + "\",\"" + UUID.randomUUID() + "\"]}")
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("duplicate.not_found"));
    dismiss(ana, "{}")
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("duplicate.not_found"));
  }

  @Test
  void r_025_01_a_body_that_is_not_json_or_has_other_properties_answers_400() throws Exception {
    dismiss(ana, "{\"contactIds\":[\"not-an-id\"]}").andExpect(status().isBadRequest());
    dismiss(ana, "{\"contactIds\":[],\"x\":1}").andExpect(status().isBadRequest());
  }
}
