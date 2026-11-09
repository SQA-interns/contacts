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
import si.contactgroups.domain.Role;
import si.contactgroups.domain.User;
import si.contactgroups.support.WebTestSupport;

/** US-017 (T-48): the Group activity list over the REST interface. */
class ActivityEndpointsIT extends WebTestSupport {

  @Autowired private JdbcClient jdbc;

  private User ana;
  private User boris;
  private User viola;
  private UUID acme;

  @BeforeEach
  void anaOwnsAcme() throws Exception {
    ana = newUser("Ana Novak");
    boris = newUser("Boris Kos");
    viola = newUser("Viola Vidmar");
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
    join(viola, Role.VIEWER);
  }

  private void join(User user, Role role) {
    jdbc.sql("INSERT INTO member (group_id, user_id, role) VALUES (:g, :u, :r)")
        .param("g", acme)
        .param("u", user.id())
        .param("r", role.name())
        .update();
  }

  private void create(User who, String first) throws Exception {
    mvc.perform(
            post("/api/v1/groups/" + acme + "/contacts")
                .with(as(who))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"" + first + "\"}"))
        .andExpect(status().isCreated());
  }

  private ResultActions activity(User who, String query) throws Exception {
    return mvc.perform(get("/api/v1/groups/" + acme + "/activity" + query).with(as(who)));
  }

  @Test
  void the_list_has_the_entries_of_all_users_newest_first_with_the_page_fields() throws Exception {
    create(ana, "Prva");
    create(boris, "Druga");

    activity(ana, "")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.pageSize").value(25))
        .andExpect(jsonPath("$.items[0].userName").value("Boris Kos"))
        .andExpect(jsonPath("$.items[1].userName").value("Ana Novak"));
  }

  @Test
  void the_filters_user_and_action_are_combined() throws Exception {
    create(ana, "Prva");
    create(boris, "Druga");

    activity(ana, "?userId=" + boris.id() + "&action=CONTACT_CREATED")
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.items[0].userName").value("Boris Kos"));
    activity(ana, "?userId=" + boris.id() + "&action=CONTACTS_EXPORTED")
        .andExpect(jsonPath("$.total").value(0));
  }

  @Test
  void a_start_after_the_end_is_422_without_a_list() throws Exception {
    activity(ana, "?from=2026-11-10&to=2026-11-01")
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("activity.range_invalid"))
        .andExpect(jsonPath("$.items").doesNotExist());
  }

  @Test
  void a_malformed_filter_value_is_400() throws Exception {
    activity(ana, "?from=yesterday").andExpect(status().isBadRequest());
    activity(ana, "?userId=x").andExpect(status().isBadRequest());
    activity(ana, "?action=NOPE").andExpect(status().isBadRequest());
  }

  @Test
  void a_viewer_is_refused_and_a_foreign_group_is_not_found() throws Exception {
    activity(viola, "").andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/groups/" + UUID.randomUUID() + "/activity").with(as(ana)))
        .andExpect(status().isNotFound());
  }
}
