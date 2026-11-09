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

/** US-026: the merge over the REST interface ({@code POST /groups/{g}/merges}). */
class MergeEndpointsIT extends WebTestSupport {

  @Autowired private JdbcClient jdbc;
  @Autowired private GroupService groups;
  @Autowired private ContactService contacts;

  private User ana;
  private UUID acme;
  private UUID a;
  private UUID b;

  @BeforeEach
  void anaOwnsAcmeWithTwoContacts() {
    ana = newUser("Ana Novak");
    acme = groups.createGroup(ana.id(), "Acme").id();
    a = contact("Ana", null);
    b = contact("ana", "040 123 456");
  }

  private UUID contact(String firstName, String phone) {
    ContactData data =
        new ContactData(
            firstName, null, "ana@acme.com", phone, null, null, null, null, null, null, null);
    return contacts.create(acme, ana.id(), data, null, null).id();
  }

  private ResultActions merge(User user, String body) throws Exception {
    return mvc.perform(
        post("/api/v1/groups/" + acme + "/merges")
            .with(as(user))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private String body(String fields) {
    return "{\"survivorId\":\""
        + a
        + "\",\"contacts\":[{\"id\":\""
        + a
        + "\",\"version\":1},{\"id\":\""
        + b
        + "\",\"version\":1}],\"fields\":"
        + fields
        + ",\"attributes\":[]}";
  }

  @Test
  void r_026_05_a_confirmed_merge_answers_200_with_the_survivor() throws Exception {
    merge(ana, body("{\"firstName\":\"Ana\",\"email\":\"ana@acme.com\",\"phone\":\"040 123 456\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(a.toString()))
        .andExpect(jsonPath("$.version").value(2))
        .andExpect(jsonPath("$.phone").value("040 123 456"));
  }

  @Test
  void r_026_12_a_viewer_is_refused() throws Exception {
    User viola = newUser("Viola Vidmar");
    jdbc.sql("INSERT INTO member (group_id, user_id, role) VALUES (:g, :u, :r)")
        .param("g", acme)
        .param("u", viola.id())
        .param("r", Role.VIEWER.name())
        .update();

    merge(viola, body("{\"firstName\":\"Ana\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("auth.role_forbidden"));
  }

  @Test
  void r_026_09_a_request_with_one_contact_is_refused_with_merge_invalid_selection()
      throws Exception {
    String one =
        "{\"survivorId\":\""
            + a
            + "\",\"contacts\":[{\"id\":\""
            + a
            + "\",\"version\":1}],\"fields\":{\"firstName\":\"Ana\"},\"attributes\":[]}";

    merge(ana, one)
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("merge.invalid_selection"));
  }

  @Test
  void d_19_d_a_value_no_merged_contact_has_is_refused_with_merge_invalid_value() throws Exception {
    merge(ana, body("{\"firstName\":\"Anja\",\"email\":\"ana@acme.com\"}"))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("merge.invalid_value"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("firstName"));
  }

  @Test
  void r_026_06_a_result_that_breaks_br_06_is_refused_with_validation_failed() throws Exception {
    merge(ana, body("{\"phone\":\"040 123 456\"}"))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("validation.failed"))
        .andExpect(jsonPath("$.fieldErrors[0].code").value("validation.required_any"));
  }

  @Test
  void r_026_08_a_contact_with_another_version_is_refused_with_409() throws Exception {
    String stale = body("{\"firstName\":\"Ana\"}").replace("\"version\":1}", "\"version\":7}");

    merge(ana, stale)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("contact.changed_by_someone_else"));
  }

  @Test
  void a_property_the_contract_does_not_know_is_refused_with_400() throws Exception {
    merge(
            ana,
            body("{\"firstName\":\"Ana\"}").replace("\"attributes\"", "\"extra\":1,\"attributes\""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("request.unknown_property"));
  }
}
