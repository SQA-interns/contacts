package si.contactgroups.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import si.contactgroups.domain.User;
import si.contactgroups.support.WebTestSupport;

/** US-006 (T-24): the Tag list, rename and delete, including what the other stories hide. */
class TagListEndpointsIT extends WebTestSupport {

  @Autowired private JdbcClient jdbc;

  private User ana;
  private UUID acme;
  private String contact;
  private String tag;

  @BeforeEach
  void anaHasAContactWithATag() throws Exception {
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
    contact =
        com.jayway.jsonpath.JsonPath.read(
            mvc.perform(
                    post("/api/v1/groups/" + acme + "/contacts")
                        .with(as(ana))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Ana\",\"tags\":[\"gost\"]}"))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.id");
    tag =
        jdbc.sql("SELECT id FROM tag WHERE group_id = :g")
            .param("g", acme)
            .query(UUID.class)
            .single()
            .toString();
  }

  private String path() {
    return "/api/v1/groups/" + acme + "/tags/" + tag;
  }

  private ResultActions rename(User who, String body) throws Exception {
    return mvc.perform(
        patch(path()).with(as(who)).contentType(MediaType.APPLICATION_JSON).content(body));
  }

  private int historyCount(String action) {
    return jdbc.sql("SELECT count(*) FROM history_entry WHERE action = :a AND group_id = :g")
        .param("a", action)
        .param("g", acme)
        .query(Integer.class)
        .single();
  }

  @Test
  void r_006_08_a_viewer_is_refused_and_nothing_changes() throws Exception {
    User viola = newUser("Viola Vidmar");
    jdbc.sql("INSERT INTO member (group_id, user_id, role) VALUES (:g, :u, 'VIEWER')")
        .param("g", acme)
        .param("u", viola.id())
        .update();

    rename(viola, "{\"name\":\"nov\"}").andExpect(status().isForbidden());
    mvc.perform(delete(path()).with(as(viola))).andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/groups/" + acme + "/tags").with(as(viola)))
        .andExpect(status().isForbidden());

    assertThat(
            jdbc.sql("SELECT name FROM tag WHERE group_id = :g")
                .param("g", acme)
                .query(String.class)
                .single())
        .isEqualTo("gost");
  }

  @Test
  void r_006_05_a_contact_in_trash_loses_the_tag_and_every_contact_gets_a_history_entry()
      throws Exception {
    jdbc.sql("UPDATE contact SET deleted_at = now() WHERE id = :c")
        .param("c", UUID.fromString(contact))
        .update();
    mvc.perform(get("/api/v1/groups/" + acme + "/tags").with(as(ana)))
        .andExpect(jsonPath("$[0].contactCount").value(1));

    mvc.perform(delete(path()).with(as(ana))).andExpect(status().isNoContent());

    assertThat(
            jdbc.sql("SELECT count(*) FROM contact_tag WHERE contact_id = :c")
                .param("c", UUID.fromString(contact))
                .query(Integer.class)
                .single())
        .isZero();
    assertThat(
            jdbc.sql("SELECT count(*) FROM contact WHERE id = :c")
                .param("c", UUID.fromString(contact))
                .query(Integer.class)
                .single())
        .isOne();
    assertThat(historyCount("TAG_DELETED")).isOne();
  }

  @Test
  void a_tag_id_that_is_not_a_uuid_or_an_unknown_property_is_refused() throws Exception {
    mvc.perform(
            patch("/api/v1/groups/" + acme + "/tags/nope")
                .with(as(ana))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"nov\"}"))
        .andExpect(status().isNotFound());
    rename(ana, "{\"name\":\"nov\",\"extra\":1}").andExpect(status().isBadRequest());
    rename(ana, "{}")
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
  }
}
