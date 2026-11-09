package si.contactgroups.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.ResultActions;
import si.contactgroups.domain.Role;
import si.contactgroups.domain.User;
import si.contactgroups.support.WebTestSupport;

/** US-016 (T-18, T-19): History entries of a Contact over the REST interface. */
class HistoryEndpointsIT extends WebTestSupport {

  @Autowired private JdbcClient jdbc;

  private User ana;
  private User boris;
  private UUID acme;

  @BeforeEach
  void anaOwnsAcmeBorisEdits() throws Exception {
    ana = newUser("Ana Novak");
    boris = newUser("Boris Kos");
    mvc.perform(
            post("/api/v1/groups")
                .with(as(ana))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Acme\"}"))
        .andExpect(status().isCreated());
    acme =
        jdbc.sql("SELECT group_id FROM member WHERE user_id = :u")
            .param("u", ana.id())
            .query(UUID.class)
            .single();
    join(boris, Role.EDITOR);
  }

  private void join(User user, Role role) {
    jdbc.sql("INSERT INTO member (group_id, user_id, role) VALUES (:g, :u, :r)")
        .param("g", acme)
        .param("u", user.id())
        .param("r", role.name())
        .update();
  }

  private String create(String body) throws Exception {
    String json =
        mvc.perform(
                post("/api/v1/groups/" + acme + "/contacts")
                    .with(as(ana))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(json, "$.id");
  }

  private ResultActions edit(User who, String id, int version, String body) throws Exception {
    return mvc.perform(
        put("/api/v1/groups/" + acme + "/contacts/" + id)
            .with(as(who))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body.replaceFirst("\\{", "{\"version\":" + version + ",")));
  }

  private ResultActions history(User who, String id, int page) throws Exception {
    return mvc.perform(
        get("/api/v1/groups/" + acme + "/contacts/" + id + "/history?page=" + page).with(as(who)));
  }

  @Test
  void ac_016_01_creating_a_contact_records_one_created_entry_with_the_stored_values()
      throws Exception {
    String id = create("{\"firstName\":\"Ana\",\"lastName\":\"Novak\",\"company\":\"Acme\"}");

    history(ana, id, 1)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.pageSize").value(25))
        .andExpect(jsonPath("$.items[0].action").value("CONTACT_CREATED"))
        .andExpect(jsonPath("$.items[0].userName").value("Ana Novak"))
        .andExpect(jsonPath("$.items[0].userEmail").value(ana.email()))
        .andExpect(jsonPath("$.items[0].contactId").value(id))
        .andExpect(jsonPath("$.items[0].details.values.company").value("Acme"));
  }

  @Test
  void ac_016_02_an_edit_lists_the_changed_field_only() throws Exception {
    String id = create("{\"firstName\":\"Ana\",\"lastName\":\"Novak\",\"company\":\"Acme\"}");

    edit(boris, id, 1, "{\"firstName\":\"Ana\",\"lastName\":\"Kos\",\"company\":\"Acme\"}")
        .andExpect(status().isOk());

    history(ana, id, 1)
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.items[0].action").value("CONTACT_EDITED"))
        .andExpect(jsonPath("$.items[0].userName").value("Boris Kos"))
        .andExpect(jsonPath("$.items[0].details.changes.lastName.old").value("Novak"))
        .andExpect(jsonPath("$.items[0].details.changes.lastName.new").value("Kos"))
        .andExpect(jsonPath("$.items[0].details.changes.company").doesNotExist());
  }

  @Test
  void ex_016_02_a_field_filled_for_the_first_time_has_an_empty_old_value() throws Exception {
    String id = create("{\"firstName\":\"Ana\"}");

    edit(ana, id, 1, "{\"firstName\":\"Ana\",\"jobTitle\":\"Vodja\"}").andExpect(status().isOk());

    history(ana, id, 1)
        .andExpect(
            jsonPath("$.items[0].details.changes.jobTitle.old")
                .value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.items[0].details.changes.jobTitle.new").value("Vodja"));
  }

  @Test
  void ex_016_03_five_changed_fields_give_one_entry_with_five_pairs() throws Exception {
    String id = create("{\"firstName\":\"Ana\"}");

    edit(
            ana,
            id,
            1,
            "{\"firstName\":\"Ana\",\"jobTitle\":\"Vodja\",\"street\":\"Ulica 1\","
                + "\"postalCode\":\"2000\",\"city\":\"Maribor\",\"notes\":\"x\"}")
        .andExpect(status().isOk());

    history(ana, id, 1)
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.items[0].details.changes.length()").value(5));
  }

  @Test
  void ac_016_08_and_ex_016_01_a_save_without_change_adds_no_entry() throws Exception {
    String id = create("{\"firstName\":\"Ana\",\"email\":\"ana@x.si\"}");

    edit(ana, id, 1, "{\"firstName\":\"Ana\",\"email\":\"ANA@x.si\"}").andExpect(status().isOk());

    history(ana, id, 1).andExpect(jsonPath("$.total").value(1));
  }

  @Test
  void ex_016_07_long_values_are_stored_in_full() throws Exception {
    String oldNotes = "a".repeat(5000);
    String newNotes = "b".repeat(5000);
    String id = create("{\"firstName\":\"Ana\",\"notes\":\"" + oldNotes + "\"}");

    edit(ana, id, 1, "{\"firstName\":\"Ana\",\"notes\":\"" + newNotes + "\"}")
        .andExpect(status().isOk());

    history(ana, id, 1)
        .andExpect(jsonPath("$.items[0].details.changes.notes.old").value(oldNotes))
        .andExpect(jsonPath("$.items[0].details.changes.notes.new").value(newNotes));
  }

  @Test
  void ac_016_04_a_viewer_is_refused() throws Exception {
    String id = create("{\"firstName\":\"Ana\"}");
    User viola = newUser("Viola Vidmar");
    join(viola, Role.VIEWER);

    history(viola, id, 1)
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("auth.role_forbidden"));
  }

  @Test
  void ac_016_05_no_request_changes_or_deletes_an_entry() throws Exception {
    String id = create("{\"firstName\":\"Ana\"}");
    String base = "/api/v1/groups/" + acme + "/contacts/" + id + "/history";

    mvc.perform(put(base).with(as(ana)).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isMethodNotAllowed());
    mvc.perform(patch(base).with(as(ana)).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isMethodNotAllowed());
    mvc.perform(delete(base).with(as(ana))).andExpect(status().isMethodNotAllowed());
    mvc.perform(delete(base + "/" + UUID.randomUUID()).with(as(ana)))
        .andExpect(status().isMethodNotAllowed());

    history(ana, id, 1).andExpect(jsonPath("$.total").value(1));
  }

  @Test
  void ac_016_06_an_entry_keeps_the_name_and_email_of_the_time() throws Exception {
    String id = create("{\"firstName\":\"Ana\"}");
    edit(boris, id, 1, "{\"firstName\":\"Ana\",\"city\":\"Celje\"}").andExpect(status().isOk());

    jdbc.sql("UPDATE app_user SET display_name = 'Boris Novi' WHERE id = :u")
        .param("u", boris.id())
        .update();
    jdbc.sql("DELETE FROM member WHERE user_id = :u").param("u", boris.id()).update();

    history(ana, id, 1)
        .andExpect(jsonPath("$.items[0].userName").value("Boris Kos"))
        .andExpect(jsonPath("$.items[0].userEmail").value(boris.email()));
  }

  @Test
  void ex_016_05_the_history_of_a_contact_of_another_group_is_not_found() throws Exception {
    String id = create("{\"firstName\":\"Ana\"}");
    User cene = newUser("Cene Zupan");
    mvc.perform(
            post("/api/v1/groups")
                .with(as(cene))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Beta\"}"))
        .andExpect(status().isCreated());
    UUID beta =
        jdbc.sql("SELECT group_id FROM member WHERE user_id = :u")
            .param("u", cene.id())
            .query(UUID.class)
            .single();

    mvc.perform(get("/api/v1/groups/" + beta + "/contacts/" + id + "/history").with(as(cene)))
        .andExpect(status().isNotFound());
  }

  @Test
  void ex_016_06_and_ac_016_09_paging_reaches_every_entry_including_the_oldest() throws Exception {
    String id = create("{\"firstName\":\"Ana\"}");
    for (int i = 1; i <= 59; i++) {
      edit(ana, id, i, "{\"firstName\":\"Ana\",\"city\":\"Kraj" + i + "\"}")
          .andExpect(status().isOk());
    }

    history(ana, id, 1)
        .andExpect(jsonPath("$.total").value(60))
        .andExpect(jsonPath("$.items.length()").value(25));
    history(ana, id, 3)
        .andExpect(jsonPath("$.items.length()").value(10))
        .andExpect(jsonPath("$.items[9].action").value("CONTACT_CREATED"));
    history(ana, id, 4).andExpect(jsonPath("$.items.length()").value(0));
  }

  @Test
  void a_failed_save_leaves_no_entry() throws Exception {
    String id = create("{\"firstName\":\"Ana\"}");

    edit(ana, id, 1, "{\"firstName\":\"Ana\",\"email\":\"ana@\"}")
        .andExpect(status().isUnprocessableContent());
    edit(ana, id, 7, "{\"firstName\":\"Ana\",\"city\":\"Celje\"}").andExpect(status().isConflict());

    history(ana, id, 1).andExpect(jsonPath("$.total").value(1));
  }
}
