package si.contactgroups.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.ResultActions;
import si.contactgroups.domain.Role;
import si.contactgroups.domain.User;
import si.contactgroups.service.GroupService;
import si.contactgroups.support.WebTestSupport;

/** US-020: {@code POST /groups/{g}/trash}, {@code GET /group-trash}, restore. */
class GroupTrashEndpointsIT extends WebTestSupport {

  @Autowired private GroupService groups;
  @Autowired private JdbcClient jdbc;

  private UUID newGroup(User owner, String name) {
    return groups.createGroup(owner.id(), name).id();
  }

  private void addMember(UUID groupId, User user, Role role) {
    jdbc.sql(
            "INSERT INTO member (group_id, user_id, role, joined_at)"
                + " VALUES (:g, :u, :r, now())")
        .param("g", groupId)
        .param("u", user.id())
        .param("r", role.name())
        .update();
  }

  private ResultActions trash(User user, UUID groupId, String typed) throws Exception {
    return mvc.perform(
        post("/api/v1/groups/" + groupId + "/trash")
            .with(as(user))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"" + typed + "\"}"));
  }

  @Test
  void r_020_01_the_typed_name_moves_the_group_to_trash_and_answers_204() throws Exception {
    User ana = newUser("Ana Novak");
    UUID acme = newGroup(ana, "Acme");

    trash(ana, acme, "Acme").andExpect(status().isNoContent());

    mvc.perform(get("/api/v1/groups").with(as(ana))).andExpect(jsonPath("$.length()").value(0));
    mvc.perform(get("/api/v1/group-trash").with(as(ana)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(acme.toString()))
        .andExpect(jsonPath("$[0].name").value("Acme"))
        .andExpect(jsonPath("$[0].daysLeft").value(30))
        .andExpect(jsonPath("$[0].deletedAt").exists());
  }

  @Test
  void r_020_02_another_name_answers_422_group_name_mismatch() throws Exception {
    User ana = newUser("Ana Novak");
    UUID acme = newGroup(ana, "Acme");

    trash(ana, acme, "acme")
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("group.name_mismatch"));

    mvc.perform(get("/api/v1/groups").with(as(ana))).andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void r_020_06_an_editor_is_refused_with_403() throws Exception {
    User ana = newUser("Ana Novak");
    User boris = newUser("Boris Kos");
    UUID acme = newGroup(ana, "Acme");
    addMember(acme, boris, Role.EDITOR);

    trash(boris, acme, "Acme")
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("auth.role_forbidden"));
  }

  @Test
  void r_020_03_every_group_endpoint_answers_404_while_the_group_is_in_trash() throws Exception {
    User ana = newUser("Ana Novak");
    UUID acme = newGroup(ana, "Acme");
    trash(ana, acme, "Acme").andExpect(status().isNoContent());

    mvc.perform(get("/api/v1/groups/" + acme + "/contacts").with(as(ana)))
        .andExpect(status().isNotFound());
    trash(ana, acme, "Acme").andExpect(status().isNotFound());
  }

  @Test
  void r_020_07_an_owner_restores_the_group_and_gets_it_as_in_the_group_list() throws Exception {
    User ana = newUser("Ana Novak");
    UUID acme = newGroup(ana, "Acme");
    trash(ana, acme, "Acme").andExpect(status().isNoContent());

    mvc.perform(post("/api/v1/group-trash/" + acme + "/restore").with(as(ana)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(acme.toString()))
        .andExpect(jsonPath("$.name").value("Acme"))
        .andExpect(jsonPath("$.role").value("OWNER"));

    mvc.perform(get("/api/v1/group-trash").with(as(ana)))
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void r_020_07_a_member_who_is_no_owner_gets_404_on_restore() throws Exception {
    User ana = newUser("Ana Novak");
    User boris = newUser("Boris Kos");
    UUID acme = newGroup(ana, "Acme");
    addMember(acme, boris, Role.EDITOR);
    trash(ana, acme, "Acme").andExpect(status().isNoContent());

    mvc.perform(post("/api/v1/group-trash/" + acme + "/restore").with(as(boris)))
        .andExpect(status().isNotFound());
  }

  @Test
  void r_020_08_a_taken_name_answers_409_group_name_taken() throws Exception {
    User ana = newUser("Ana Novak");
    UUID acme = newGroup(ana, "Acme");
    trash(ana, acme, "Acme").andExpect(status().isNoContent());
    newGroup(ana, "ACME");

    mvc.perform(post("/api/v1/group-trash/" + acme + "/restore").with(as(ana)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("group.name_taken"));
  }

  @Test
  void r_020_07_an_id_that_is_not_a_group_id_answers_404() throws Exception {
    User ana = newUser("Ana Novak");

    mvc.perform(post("/api/v1/group-trash/not-an-id/restore").with(as(ana)))
        .andExpect(status().isNotFound());
  }
}
