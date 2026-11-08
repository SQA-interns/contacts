package si.contactgroups.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import si.contactgroups.domain.Role;
import si.contactgroups.domain.User;
import si.contactgroups.support.WebTestSupport;

/**
 * US-018 T-07: the check of Membership, Role and verified e-mail on every endpoint with a Group in
 * its path (R-018-02, R-018-03, R-018-06, EX-018-01, EX-018-02, EX-018-05, EX-018-08), run against
 * the test-only endpoints of {@code GroupPathProbe}.
 */
class GroupAccessIT extends WebTestSupport {

  @Autowired private JdbcClient jdbc;

  private UUID groupOf(User owner, String name) throws Exception {
    mvc.perform(
            post("/api/v1/groups")
                .with(as(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\"}"))
        .andExpect(status().isCreated());
    return jdbc.sql(
            "SELECT g.id FROM contact_group g JOIN member m ON m.group_id = g.id"
                + " WHERE m.user_id = :u AND g.name = :n")
        .param("u", owner.id())
        .param("n", name)
        .query(UUID.class)
        .single();
  }

  private void join(UUID group, User user, Role role) {
    jdbc.sql("INSERT INTO member (group_id, user_id, role) VALUES (:g, :u, :r)")
        .param("g", group)
        .param("u", user.id())
        .param("r", role.name())
        .update();
  }

  private static String probe(Object groupId) {
    return "/api/v1/groups/" + groupId + "/probe";
  }

  @Test
  void r_018_02_a_member_reaches_the_endpoint_of_the_group() throws Exception {
    User ana = newUser("Ana Novak");
    UUID acme = groupOf(ana, "Acme");

    mvc.perform(get(probe(acme)).with(as(ana)))
        .andExpect(status().isOk())
        .andExpect(content().string("read " + acme));
  }

  @Test
  void r_018_02_a_user_who_is_no_member_gets_404_group_not_found_without_data() throws Exception {
    User ana = newUser("Ana Novak");
    User boris = newUser("Boris Kos");
    UUID acme = groupOf(ana, "Acme");

    mvc.perform(get(probe(acme)).with(as(boris)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("group.not_found"))
        .andExpect(
            content()
                .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Acme"))));
  }

  @Test
  void ex_018_01_a_group_id_that_does_not_exist_has_the_same_answer_as_a_foreign_group()
      throws Exception {
    User ana = newUser("Ana Novak");
    User boris = newUser("Boris Kos");
    UUID acme = groupOf(ana, "Acme");

    String foreign =
        mvc.perform(get(probe(acme)).with(as(boris)))
            .andReturn()
            .getResponse()
            .getContentAsString();

    mvc.perform(get(probe(UUID.randomUUID())).with(as(boris)))
        .andExpect(status().isNotFound())
        .andExpect(content().json(foreign, true));
  }

  @Test
  void ex_018_02_a_malformed_group_id_gets_404_in_the_error_format() throws Exception {
    User ana = newUser("Ana Novak");

    mvc.perform(get(probe("abc")).with(as(ana)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("group.not_found"))
        .andExpect(jsonPath("$.message").isNotEmpty());
  }

  @Test
  void r_018_03_a_role_that_may_not_do_the_action_gets_403_and_nothing_changes() throws Exception {
    User ana = newUser("Ana Novak");
    User viola = newUser("Viola Vidmar");
    UUID acme = groupOf(ana, "Acme");
    join(acme, viola, Role.VIEWER);

    mvc.perform(get(probe(acme)).with(as(viola))).andExpect(status().isOk());
    mvc.perform(post(probe(acme)).with(as(viola)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("auth.role_forbidden"));
    mvc.perform(post(probe(acme)).with(as(ana))).andExpect(status().isOk());
  }

  @Test
  void ex_018_05_a_member_who_was_just_removed_gets_404_on_the_next_request() throws Exception {
    User ana = newUser("Ana Novak");
    User boris = newUser("Boris Kos");
    UUID acme = groupOf(ana, "Acme");
    join(acme, boris, Role.EDITOR);
    mvc.perform(get(probe(acme)).with(as(boris))).andExpect(status().isOk());

    jdbc.sql("DELETE FROM member WHERE group_id = :g AND user_id = :u")
        .param("g", acme)
        .param("u", boris.id())
        .update();

    mvc.perform(get(probe(acme)).with(as(boris))).andExpect(status().isNotFound());
  }

  @Test
  void sr_03_the_role_is_read_from_the_database_on_every_request() throws Exception {
    User ana = newUser("Ana Novak");
    User eva = newUser("Eva Zupan");
    UUID acme = groupOf(ana, "Acme");
    join(acme, eva, Role.OWNER);
    mvc.perform(post(probe(acme)).with(as(eva))).andExpect(status().isOk());

    jdbc.sql("UPDATE member SET role = 'VIEWER' WHERE group_id = :g AND user_id = :u")
        .param("g", acme)
        .param("u", eva.id())
        .update();

    mvc.perform(post(probe(acme)).with(as(eva))).andExpect(status().isForbidden());
  }

  @Test
  void ex_018_08_a_group_in_trash_gets_404_also_for_its_owner() throws Exception {
    User ana = newUser("Ana Novak");
    UUID acme = groupOf(ana, "Acme");
    jdbc.sql("UPDATE contact_group SET deleted_at = now() WHERE id = :g").param("g", acme).update();

    mvc.perform(get(probe(acme)).with(as(ana)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("group.not_found"));
  }

  @Test
  void r_018_06_an_unverified_e_mail_gets_403_on_a_group_endpoint() throws Exception {
    User ana = newUser("Ana Novak");
    UUID acme = groupOf(ana, "Acme");

    mvc.perform(get(probe(acme)).with(as(ana, false)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("auth.email_not_verified"));
  }
}
