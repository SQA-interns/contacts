package si.contactgroups.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import si.contactgroups.domain.Role;
import si.contactgroups.domain.User;
import si.contactgroups.support.WebTestSupport;

/** US-012 (T-37, T-38): delete, Trash view and restore, their Roles, refusals and History. */
class TrashEndpointsIT extends WebTestSupport {

  @Autowired private JdbcClient jdbc;

  private User ana;
  private UUID acme;
  private String contact;

  @BeforeEach
  void anaOwnsAcmeWithOneContact() throws Exception {
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
                    .content("{\"lastName\":\"Novak\"}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    contact = JsonPath.read(json, "$.id");
  }

  private User join(String name, Role role) {
    User user = newUser(name);
    jdbc.sql("INSERT INTO member (group_id, user_id, role) VALUES (:g, :u, :r)")
        .param("g", acme)
        .param("u", user.id())
        .param("r", role.name())
        .update();
    return user;
  }

  private String url(String suffix) {
    return "/api/v1/groups/" + acme + suffix;
  }

  private int entries(String action) {
    return jdbc.sql("SELECT count(*) FROM history_entry WHERE group_id = :g AND action = :a")
        .param("g", acme)
        .param("a", action)
        .query(Integer.class)
        .single();
  }

  /** Hours, not days: a day is 23 or 25 hours across a change of summer time. */
  private void ageTrash(String hours) {
    jdbc.sql("UPDATE contact SET deleted_at = now() - CAST(:d AS interval) WHERE id = :id")
        .param("d", hours)
        .param("id", UUID.fromString(contact))
        .update();
  }

  @Test
  void aViewerMayNeitherDeleteNorOpenTheTrashNorRestore() throws Exception {
    User viola = join("Viola Vidmar", Role.VIEWER);

    mvc.perform(post(url("/contacts/" + contact + "/trash")).with(as(viola)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("auth.role_forbidden"));
    mvc.perform(get(url("/trash/contacts")).with(as(viola))).andExpect(status().isForbidden());
    mvc.perform(post(url("/trash/contacts/" + contact + "/restore")).with(as(viola)))
        .andExpect(status().isForbidden());

    assertThat(entries("CONTACT_DELETED")).isZero();
  }

  @Test
  void aDeleteIsRecordedOnceAndTheTrashViewShowsWhoDeletedIt() throws Exception {
    User boris = join("Boris Kos", Role.EDITOR);

    mvc.perform(post(url("/contacts/" + contact + "/trash")).with(as(boris)))
        .andExpect(status().isNoContent());
    mvc.perform(post(url("/contacts/" + contact + "/trash")).with(as(boris)))
        .andExpect(status().isNoContent());

    assertThat(entries("CONTACT_DELETED")).isEqualTo(1);
    mvc.perform(get(url("/trash/contacts")).with(as(ana)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].deletedBy").value("Boris Kos"))
        .andExpect(jsonPath("$[0].lastName").value("Novak"))
        .andExpect(jsonPath("$[0].daysLeft").value(30));
  }

  @Test
  void aContactThatIsNotInTheGroupIsNotFoundAndNothingIsRecorded() throws Exception {
    mvc.perform(post(url("/contacts/" + UUID.randomUUID() + "/trash")).with(as(ana)))
        .andExpect(status().isNotFound());
    mvc.perform(post(url("/contacts/not-an-id/trash")).with(as(ana)))
        .andExpect(status().isNotFound());

    assertThat(entries("CONTACT_DELETED")).isZero();
  }

  @Test
  void aContactNotInTrashCannotBeRestored() throws Exception {
    mvc.perform(post(url("/trash/contacts/" + contact + "/restore")).with(as(ana)))
        .andExpect(status().isNotFound());

    assertThat(entries("CONTACT_RESTORED")).isZero();
  }

  @Test
  void theTrashViewHidesAContactPastThe30DaysAndItCannotBeRestored() throws Exception {
    mvc.perform(post(url("/contacts/" + contact + "/trash")).with(as(ana)));
    ageTrash("720 hours");

    mvc.perform(get(url("/trash/contacts")).with(as(ana)))
        .andExpect(jsonPath("$.length()").value(0));
    mvc.perform(post(url("/trash/contacts/" + contact + "/restore")).with(as(ana)))
        .andExpect(status().isNotFound());
    assertThat(entries("CONTACT_RESTORED")).isZero();
  }

  @Test
  void aRestoreBefore30DaysKeepsTheVersionAndIsRecorded() throws Exception {
    mvc.perform(post(url("/contacts/" + contact + "/trash")).with(as(ana)));
    ageTrash("719 hours");

    mvc.perform(post(url("/trash/contacts/" + contact + "/restore")).with(as(ana)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(1))
        .andExpect(jsonPath("$.lastName").value("Novak"));

    assertThat(entries("CONTACT_RESTORED")).isEqualTo(1);
  }

  @Test
  void aSaveOfAContactInTrashIsRefusedWithConflictAndStoresNothing() throws Exception {
    mvc.perform(post(url("/contacts/" + contact + "/trash")).with(as(ana)));

    mvc.perform(
            put(url("/contacts/" + contact))
                .with(as(ana))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lastName\":\"Kos\",\"version\":1}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("contact.in_trash"));

    Instant deleted =
        jdbc.sql("SELECT deleted_at FROM contact WHERE id = :id")
            .param("id", UUID.fromString(contact))
            .query(java.sql.Timestamp.class)
            .single()
            .toInstant();
    assertThat(deleted).isNotNull();
    assertThat(
            jdbc.sql("SELECT last_name FROM contact WHERE id = :id")
                .param("id", UUID.fromString(contact))
                .query(String.class)
                .single())
        .isEqualTo("Novak");
  }

  @Test
  void aSaveOfAContactOfAnotherGroupStaysNotFound() throws Exception {
    mvc.perform(
            put(url("/contacts/" + UUID.randomUUID()))
                .with(as(ana))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lastName\":\"Kos\",\"version\":1}"))
        .andExpect(status().isNotFound());
  }
}
