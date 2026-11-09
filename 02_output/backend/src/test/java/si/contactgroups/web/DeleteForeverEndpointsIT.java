package si.contactgroups.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import si.contactgroups.domain.Role;
import si.contactgroups.domain.User;
import si.contactgroups.job.TrashPurgeJob;
import si.contactgroups.support.WebTestSupport;

/** US-013 (T-40, T-41): delete forever by the Owner and by the clean-up, History and Roles. */
class DeleteForeverEndpointsIT extends WebTestSupport {

  @Autowired private JdbcClient jdbc;
  @Autowired private TrashPurgeJob job;

  private User ana;
  private UUID acme;
  private String contact;

  @BeforeEach
  void anaOwnsAcmeWithMojcaWhoHadAnEdit() throws Exception {
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
    String json =
        mvc.perform(
                post("/api/v1/groups/" + acme + "/contacts")
                    .with(as(ana))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"firstName\":\"Mojca\",\"lastName\":\"Zupan\"}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    contact = JsonPath.read(json, "$.id");
    mvc.perform(
            put(url("/contacts/" + contact))
                .with(as(ana))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Mojca\",\"lastName\":\"Kos\",\"version\":1}"))
        .andExpect(status().isOk());
  }

  private String url(String suffix) {
    return "/api/v1/groups/" + acme + suffix;
  }

  private void toTrash() throws Exception {
    mvc.perform(post(url("/contacts/" + contact + "/trash")).with(as(ana)))
        .andExpect(status().isNoContent());
  }

  private int rows(String table, String column) {
    return jdbc.sql("SELECT count(*) FROM " + table + " WHERE " + column + " = :id")
        .param("id", UUID.fromString(contact))
        .query(Integer.class)
        .single();
  }

  private List<String> detailsOf(String action) {
    return jdbc.sql(
            "SELECT CAST(details AS text) FROM history_entry WHERE group_id = :g AND action = :a")
        .param("g", acme)
        .param("a", action)
        .query(String.class)
        .list();
  }

  @Test
  void anEditorAndAViewerAreRefusedAndTheContactStaysInTrash() throws Exception {
    toTrash();
    for (Role role : List.of(Role.EDITOR, Role.VIEWER)) {
      User user = newUser(role.name());
      jdbc.sql("INSERT INTO member (group_id, user_id, role) VALUES (:g, :u, :r)")
          .param("g", acme)
          .param("u", user.id())
          .param("r", role.name())
          .update();
      mvc.perform(delete(url("/trash/contacts/" + contact)).with(as(user)))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("auth.role_forbidden"));
    }

    assertThat(rows("contact", "id")).isEqualTo(1);
    assertThat(detailsOf("CONTACT_DELETED_FOREVER")).isEmpty();
  }

  @Test
  void aContactNotInTrashIsRefusedAndUnchanged() throws Exception {
    mvc.perform(delete(url("/trash/contacts/" + contact)).with(as(ana)))
        .andExpect(status().isNotFound());

    assertThat(rows("contact", "id")).isEqualTo(1);
  }

  @Test
  void theOwnerDeletesForeverTheSecondRequestIsNotFoundAndNoValueIsKept() throws Exception {
    toTrash();

    mvc.perform(delete(url("/trash/contacts/" + contact)).with(as(ana)))
        .andExpect(status().isNoContent());
    mvc.perform(delete(url("/trash/contacts/" + contact)).with(as(ana)))
        .andExpect(status().isNotFound());

    assertThat(rows("contact", "id")).isZero();
    assertThat(rows("contact_version", "contact_id")).isZero();
    assertThat(detailsOf("CONTACT_DELETED_FOREVER")).containsExactly("{}");
    assertThat(detailsOf("CONTACT_EDITED"))
        .hasSize(1)
        .allSatisfy(d -> assertThat(d).contains("removed").doesNotContain("Zupan", "Kos"));
    assertThat(detailsOf("CONTACT_CREATED"))
        .allSatisfy(d -> assertThat(d).doesNotContain("Mojca", "Zupan"));
    assertThat(
            jdbc.sql(
                    "SELECT user_name FROM history_entry WHERE group_id = :g"
                        + " AND action = 'CONTACT_DELETED_FOREVER'")
                .param("g", acme)
                .query(String.class)
                .single())
        .isEqualTo("Ana Novak");
  }

  @Test
  void theCleanUpDeletesAfter30DaysAsSystemAndKeepsAYoungerOne() throws Exception {
    toTrash();
    jdbc.sql("UPDATE contact SET deleted_at = now() - interval '719 hours' WHERE id = :id")
        .param("id", UUID.fromString(contact))
        .update();
    job.purgeOnce();
    assertThat(rows("contact", "id")).isEqualTo(1);

    jdbc.sql("UPDATE contact SET deleted_at = now() - interval '720 hours' WHERE id = :id")
        .param("id", UUID.fromString(contact))
        .update();
    job.purgeOnce();

    assertThat(rows("contact", "id")).isZero();
    assertThat(
            jdbc.sql(
                    "SELECT user_name || '/' || coalesce(user_email, 'none')"
                        + " FROM history_entry WHERE group_id = :g"
                        + " AND action = 'CONTACT_DELETED_FOREVER'")
                .param("g", acme)
                .query(String.class)
                .single())
        .isEqualTo("system/none");
    assertThat(detailsOf("CONTACT_EDITED")).allSatisfy(d -> assertThat(d).doesNotContain("Kos"));
  }
}
