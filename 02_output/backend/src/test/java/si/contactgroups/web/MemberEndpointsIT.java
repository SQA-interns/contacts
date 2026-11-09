package si.contactgroups.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.ResultActions;
import si.contactgroups.domain.User;
import si.contactgroups.support.WebTestSupport;

/** US-015 (T-46): Member list, Role change and removal with the last-Owner rule. */
class MemberEndpointsIT extends WebTestSupport {

  @Autowired private JdbcClient jdbc;

  private User ana;
  private User boris;
  private UUID acme;

  @BeforeEach
  void anaOwnsAcmeWithBoris() throws Exception {
    ana = newUser("Ana Novak");
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
    boris = newUser("Boris Kos");
    addMember(boris, "EDITOR");
  }

  private void addMember(User user, String role) {
    jdbc.sql("INSERT INTO member (group_id, user_id, role, joined_at)" + " VALUES (:g, :u, :r, :t)")
        .param("g", acme)
        .param("u", user.id())
        .param("r", role)
        .param("t", java.sql.Timestamp.from(Instant.now()))
        .update();
  }

  private String url(User member) {
    return "/api/v1/groups/" + acme + "/members/" + member.id();
  }

  private ResultActions changeRole(User by, User member, String role) throws Exception {
    return mvc.perform(
        patch(url(member))
            .with(as(by))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"role\":\"" + role + "\"}"));
  }

  private int historyCount(String action) {
    return jdbc.sql("SELECT count(*) FROM history_entry WHERE group_id = :g AND action = :a")
        .param("g", acme)
        .param("a", action)
        .query(Integer.class)
        .single();
  }

  @Test
  void every_member_lists_the_members_with_name_email_and_role() throws Exception {
    mvc.perform(get("/api/v1/groups/" + acme + "/members").with(as(boris)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[?(@.displayName=='Ana Novak')].role").value("OWNER"))
        .andExpect(jsonPath("$[?(@.displayName=='Boris Kos')].email").value(boris.email()));
  }

  @Test
  void owner_changes_a_role_and_a_history_entry_with_old_and_new_role_is_written()
      throws Exception {
    changeRole(ana, boris, "VIEWER")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("VIEWER"));

    assertThat(historyCount("ROLE_CHANGED")).isEqualTo(1);
    String details =
        jdbc.sql(
                "SELECT details::text FROM history_entry WHERE group_id = :g AND action = 'ROLE_CHANGED'")
            .param("g", acme)
            .query(String.class)
            .single();
    assertThat(details).contains("EDITOR").contains("VIEWER");
  }

  @Test
  void same_role_changes_nothing_and_writes_no_entry() throws Exception {
    changeRole(ana, boris, "EDITOR").andExpect(status().isOk());

    assertThat(historyCount("ROLE_CHANGED")).isZero();
  }

  @Test
  void the_last_owner_cannot_be_demoted_or_removed() throws Exception {
    changeRole(ana, ana, "EDITOR")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("group.last_owner"));
    mvc.perform(delete(url(ana)).with(as(ana)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("group.last_owner"));

    assertThat(historyCount("ROLE_CHANGED")).isZero();
    assertThat(historyCount("MEMBER_REMOVED")).isZero();
  }

  @Test
  void an_owner_may_demote_herself_when_another_owner_exists() throws Exception {
    addMember(newUser("Cene Zupan"), "OWNER");

    changeRole(ana, ana, "EDITOR").andExpect(status().isOk());
  }

  @Test
  void removing_deletes_the_membership_and_writes_an_entry() throws Exception {
    mvc.perform(delete(url(boris)).with(as(ana))).andExpect(status().isNoContent());

    mvc.perform(get("/api/v1/groups/" + acme + "/members").with(as(boris)))
        .andExpect(status().isNotFound());
    assertThat(historyCount("MEMBER_REMOVED")).isEqualTo(1);
  }

  @Test
  void a_user_who_is_no_member_is_not_found() throws Exception {
    mvc.perform(delete(url(newUser("Cene Zupan"))).with(as(ana)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("member.not_found"));
    changeRole(ana, newUser("Dana Rep"), "VIEWER").andExpect(status().isNotFound());
  }

  @Test
  void a_non_owner_is_refused_and_a_malformed_user_id_is_not_found() throws Exception {
    changeRole(boris, ana, "VIEWER").andExpect(status().isForbidden());
    mvc.perform(delete("/api/v1/groups/" + acme + "/members/xyz").with(as(ana)))
        .andExpect(status().isNotFound());
  }
}
