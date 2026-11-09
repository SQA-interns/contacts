package si.contactgroups.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

/** US-005 (T-22): Tags are part of the Contact save. */
class TagEndpointsIT extends WebTestSupport {

  @Autowired private JdbcClient jdbc;

  private User ana;
  private UUID acme;
  private String contact;

  @BeforeEach
  void anaHasAContact() throws Exception {
    ana = newUser("Ana Novak");
    acme = groupOf(ana, "Acme");
    contact = idOf(create(ana, acme, "{\"firstName\":\"Ana\"}"));
  }

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

  private ResultActions create(User who, UUID group, String body) throws Exception {
    return mvc.perform(
        post("/api/v1/groups/" + group + "/contacts")
            .with(as(who))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private ResultActions save(User who, int version, String tags) throws Exception {
    String sent = tags == null ? "" : ",\"tags\":" + tags;
    return mvc.perform(
        put("/api/v1/groups/" + acme + "/contacts/" + contact)
            .with(as(who))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"firstName\":\"Ana\",\"version\":" + version + sent + "}"));
  }

  private static String idOf(ResultActions created) throws Exception {
    return com.jayway.jsonpath.JsonPath.read(
        created.andReturn().getResponse().getContentAsString(), "$.id");
  }

  private int tagRows(UUID group) {
    return jdbc.sql("SELECT count(*) FROM tag WHERE group_id = :g")
        .param("g", group)
        .query(Integer.class)
        .single();
  }

  @Test
  void r_005_01_08_a_typed_tag_is_created_put_on_the_contact_and_raises_the_version()
      throws Exception {
    save(ana, 1, "[\"udelezenec2025\"]")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(2))
        .andExpect(jsonPath("$.tags[0]").value("udelezenec2025"));

    mvc.perform(get("/api/v1/groups/" + acme + "/contacts/" + contact + "/history").with(as(ana)))
        .andExpect(jsonPath("$.items[0].action").value("TAG_ADDED"))
        .andExpect(jsonPath("$.items[0].details.name").value("udelezenec2025"));
  }

  @Test
  void r_005_03_the_existing_spelling_is_used_and_no_second_tag_is_created() throws Exception {
    save(ana, 1, "[\"neObvescaj\"]");
    String boris = idOf(create(ana, acme, "{\"firstName\":\"Boris\"}"));

    mvc.perform(
            put("/api/v1/groups/" + acme + "/contacts/" + boris)
                .with(as(ana))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Boris\",\"version\":1,\"tags\":[\"Neobvescaj\"]}"))
        .andExpect(jsonPath("$.tags[0]").value("neObvescaj"));

    org.assertj.core.api.Assertions.assertThat(tagRows(acme)).isEqualTo(1);
  }

  @Test
  void r_005_06_removing_a_tag_removes_only_the_link_and_records_it() throws Exception {
    save(ana, 1, "[\"gost\"]");
    save(ana, 2, "[]").andExpect(jsonPath("$.tags.length()").value(0));

    org.assertj.core.api.Assertions.assertThat(tagRows(acme)).isEqualTo(1);
    mvc.perform(get("/api/v1/groups/" + acme + "/contacts/" + contact + "/history").with(as(ana)))
        .andExpect(jsonPath("$.items[0].action").value("TAG_REMOVED"))
        .andExpect(jsonPath("$.items[0].details.name").value("gost"));
  }

  @Test
  void an_edit_without_the_tags_property_keeps_the_tags() throws Exception {
    save(ana, 1, "[\"gost\"]");

    save(ana, 2, null)
        .andExpect(jsonPath("$.version").value(2))
        .andExpect(jsonPath("$.tags[0]").value("gost"));
  }

  @Test
  void r_005_02_an_invalid_name_stores_nothing_and_names_the_position() throws Exception {
    save(ana, 1, "[\"ok\",\"a b\"]")
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("tags[1]"))
        .andExpect(jsonPath("$.fieldErrors[0].code").value("validation.tag_invalid"));

    org.assertj.core.api.Assertions.assertThat(tagRows(acme)).isZero();
  }

  @Test
  void r_005_05_a_tag_of_another_group_is_not_reused() throws Exception {
    UUID beta = groupOf(ana, "Beta");
    create(ana, beta, "{\"firstName\":\"Cene\",\"tags\":[\"gost\"]}")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.tags[0]").value("gost"));

    save(ana, 1, "[\"GOST\"]").andExpect(jsonPath("$.tags[0]").value("GOST"));

    org.assertj.core.api.Assertions.assertThat(tagRows(beta)).isEqualTo(1);
    org.assertj.core.api.Assertions.assertThat(tagRows(acme)).isEqualTo(1);
  }

  @Test
  void r_005_04_a_51st_tag_is_refused_with_the_limit() throws Exception {
    StringBuilder names = new StringBuilder("[");
    for (int i = 1; i <= 51; i++) {
      names.append(i > 1 ? "," : "").append("\"t").append(i).append('"');
    }
    save(ana, 1, names + "]")
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.fieldErrors[0].code").value("validation.tag_limit"));
  }

  @Test
  void r_005_10_a_viewer_is_refused() throws Exception {
    User viola = newUser("Viola Vidmar");
    jdbc.sql("INSERT INTO member (group_id, user_id, role) VALUES (:g, :u, :r)")
        .param("g", acme)
        .param("u", viola.id())
        .param("r", Role.VIEWER.name())
        .update();

    save(viola, 1, "[\"gost\"]").andExpect(status().isForbidden());
  }

  @Test
  void a_new_contact_can_carry_tags_and_the_version_snapshot_lists_them() throws Exception {
    create(ana, acme, "{\"firstName\":\"Boris\",\"tags\":[\"b\",\"A\"]}")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.tags[0]").value("A"))
        .andExpect(jsonPath("$.tags[1]").value("b"));

    org.assertj.core.api.Assertions.assertThat(
            jdbc.sql(
                    "SELECT snapshot->'tags' ->> 0 FROM contact_version v JOIN contact c ON"
                        + " c.id = v.contact_id WHERE c.first_name = 'Boris'"
                        + " AND c.group_id = :g")
                .param("g", acme)
                .query(String.class)
                .single())
        .isEqualTo("A");
  }
}
