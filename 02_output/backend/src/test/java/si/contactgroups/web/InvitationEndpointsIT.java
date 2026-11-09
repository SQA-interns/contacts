package si.contactgroups.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.ResultActions;
import si.contactgroups.domain.User;
import si.contactgroups.support.WebTestSupport;

/** US-014 (T-43, T-44): invite by e-mail address, cancel, list, join through the Group list. */
class InvitationEndpointsIT extends WebTestSupport {

  @Autowired private JdbcClient jdbc;

  private User ana;
  private UUID acme;

  @BeforeEach
  void anaOwnsAcme() throws Exception {
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
  }

  private String url() {
    return "/api/v1/groups/" + acme + "/invitations";
  }

  private ResultActions invite(User by, String email, String role) throws Exception {
    return mvc.perform(
        post(url())
            .with(as(by))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"role\":\"" + role + "\"}"));
  }

  private String inviteOk(String email, String role) throws Exception {
    String json =
        invite(ana, email, role)
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(json, "$.id");
  }

  private int pendingCount() throws Exception {
    String json =
        mvc.perform(get(url()).with(as(ana))).andReturn().getResponse().getContentAsString();
    return JsonPath.<List<Object>>read(json, "$").size();
  }

  private void backdate(String invitationId, Duration age) {
    jdbc.sql(
            "UPDATE invitation SET created_at = created_at - :a::interval,"
                + " expires_at = expires_at - :a::interval WHERE id = :id")
        .param("a", age.toSeconds() + " seconds")
        .param("id", UUID.fromString(invitationId))
        .update();
  }

  private int groupCount(User user) throws Exception {
    String json =
        mvc.perform(get("/api/v1/groups").with(as(user)))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.<List<Object>>read(json, "$").size();
  }

  @Test
  void r_014_01_creates_pending_invitation_lowercase_expiring_in_14_days() throws Exception {
    String json =
        invite(ana, "  BORIS@ACME.COM ", "EDITOR")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.email").value("boris@acme.com"))
            .andExpect(jsonPath("$.role").value("EDITOR"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    Instant created = Instant.parse(JsonPath.read(json, "$.createdAt"));
    Instant expires = Instant.parse(JsonPath.read(json, "$.expiresAt"));
    assertThat(Duration.between(created, expires)).isEqualTo(Duration.ofDays(14));
  }

  @Test
  void r_014_02_an_address_without_valid_format_creates_nothing() throws Exception {
    invite(ana, "boris@", "EDITOR")
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));

    assertThat(pendingCount()).isZero();
  }

  @Test
  void r_014_03_the_21st_pending_invitation_is_refused_and_expired_ones_do_not_count()
      throws Exception {
    String old = inviteOk("stara@acme.com", "VIEWER");
    for (int i = 1; i <= 19; i++) {
      inviteOk("oseba" + i + "@acme.com", "VIEWER");
    }
    invite(ana, "dvajsetprva@acme.com", "VIEWER")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("invitation.limit"));

    backdate(old, Duration.ofDays(14));
    inviteOk("nova@acme.com", "VIEWER");
  }

  @Test
  void r_014_04_a_member_or_a_pending_address_cannot_be_invited() throws Exception {
    invite(ana, ana.email().toUpperCase(), "EDITOR")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("invitation.exists"));
    inviteOk("boris@acme.com", "EDITOR");

    invite(ana, "Boris@acme.com", "VIEWER")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("invitation.exists"));
    assertThat(pendingCount()).isEqualTo(1);
  }

  @Test
  void r_014_05_a_verified_user_with_the_address_joins_when_opening_the_group_list()
      throws Exception {
    User boris = newUser("Boris Kos");
    inviteOk(boris.email().toUpperCase(), "EDITOR");

    mvc.perform(get("/api/v1/groups").with(as(boris)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Acme"))
        .andExpect(jsonPath("$[0].role").value("EDITOR"));

    assertThat(pendingCount()).isZero();
    assertThat(
            jdbc.sql("SELECT status FROM invitation WHERE group_id = :g")
                .param("g", acme)
                .query(String.class)
                .single())
        .isEqualTo("ACCEPTED");
    assertThat(actions()).contains("INVITATION_CREATED", "MEMBER_JOINED");
  }

  @Test
  void r_014_06_08_cancelled_and_expired_invitations_give_no_access() throws Exception {
    User cene = newUser("Cene Zupan");
    String cancelled = inviteOk(cene.email(), "EDITOR");
    mvc.perform(post(url() + "/" + cancelled + "/cancel").with(as(ana)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
    User dani = newUser("Dani Kralj");
    backdate(inviteOk(dani.email(), "EDITOR"), Duration.ofDays(14));

    assertThat(groupCount(cene)).isZero();
    assertThat(groupCount(dani)).isZero();
    assertThat(actions()).contains("INVITATION_CANCELLED").doesNotContain("MEMBER_JOINED");
  }

  @Test
  void r_014_08_an_invitation_13_days_23_hours_old_is_still_valid() throws Exception {
    User boris = newUser("Boris Kos");
    backdate(inviteOk(boris.email(), "VIEWER"), Duration.ofDays(13).plusHours(23));

    assertThat(groupCount(boris)).isEqualTo(1);
  }

  @Test
  void r_014_10_an_editor_or_a_viewer_cannot_invite_or_cancel() throws Exception {
    User boris = newUser("Boris Kos");
    inviteOk(boris.email(), "EDITOR");
    groupCount(boris);
    String id = inviteOk("cene@acme.com", "VIEWER");

    invite(boris, "dani@acme.com", "VIEWER")
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("auth.role_forbidden"));
    mvc.perform(post(url() + "/" + id + "/cancel").with(as(boris)))
        .andExpect(status().isForbidden());
    mvc.perform(get(url()).with(as(boris))).andExpect(status().isForbidden());
    assertThat(pendingCount()).isEqualTo(1);
  }

  @Test
  void r_014_07_cancelling_an_unknown_or_foreign_invitation_answers_404() throws Exception {
    mvc.perform(post(url() + "/" + UUID.randomUUID() + "/cancel").with(as(ana)))
        .andExpect(status().isNotFound());
  }

  private List<String> actions() {
    return jdbc.sql("SELECT action FROM history_entry WHERE group_id = :g")
        .param("g", acme)
        .query(String.class)
        .list();
  }
}
