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

/** US-004 (T-20): Attributes are part of the Contact save. */
class AttributeEndpointsIT extends WebTestSupport {

  private static final String PAIR = "{\"key\":\"kompetenca\",\"value\":\"java\"}";

  @Autowired private JdbcClient jdbc;

  private User ana;
  private UUID acme;
  private String contact;

  @BeforeEach
  void anaHasAContact() throws Exception {
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
    contact = idOf(create(ana, "{\"firstName\":\"Ana\"}"));
  }

  private ResultActions create(User who, String body) throws Exception {
    return mvc.perform(
        post("/api/v1/groups/" + acme + "/contacts")
            .with(as(who))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private ResultActions save(User who, int version, String attributes) throws Exception {
    String attrs = attributes == null ? "" : ",\"attributes\":" + attributes;
    return mvc.perform(
        put("/api/v1/groups/" + acme + "/contacts/" + contact)
            .with(as(who))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"firstName\":\"Ana\",\"version\":" + version + attrs + "}"));
  }

  private static String idOf(ResultActions created) throws Exception {
    return com.jayway.jsonpath.JsonPath.read(
        created.andReturn().getResponse().getContentAsString(), "$.id");
  }

  private ResultActions history() throws Exception {
    return mvc.perform(
        get("/api/v1/groups/" + acme + "/contacts/" + contact + "/history").with(as(ana)));
  }

  @Test
  void r_004_01_07_a_new_pair_is_stored_and_raises_the_version_even_when_no_field_changed()
      throws Exception {
    save(ana, 1, "[" + PAIR + "]")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(2))
        .andExpect(jsonPath("$.attributes[0].key").value("kompetenca"))
        .andExpect(jsonPath("$.attributes[0].value").value("java"));

    mvc.perform(get("/api/v1/groups/" + acme + "/contacts/" + contact).with(as(ana)))
        .andExpect(jsonPath("$.attributes.length()").value(1));
  }

  @Test
  void r_004_01_a_Contact_can_be_created_with_pairs() throws Exception {
    create(ana, "{\"firstName\":\"Boris\",\"attributes\":[" + PAIR + "]}")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.attributes[0].key").value("kompetenca"));
  }

  @Test
  void r_004_06_add_change_and_remove_are_recorded_with_key_old_and_new_value() throws Exception {
    save(ana, 1, "[" + PAIR + "]");
    save(ana, 2, "[{\"key\":\"kompetenca\",\"value\":\"sql\"}]");
    save(ana, 3, "[]").andExpect(jsonPath("$.attributes.length()").value(0));

    history()
        .andExpect(jsonPath("$.items[2].action").value("ATTRIBUTE_ADDED"))
        .andExpect(jsonPath("$.items[2].details.key").value("kompetenca"))
        .andExpect(jsonPath("$.items[2].details.new").value("java"))
        .andExpect(jsonPath("$.items[1].action").value("ATTRIBUTE_CHANGED"))
        .andExpect(jsonPath("$.items[1].details.old").value("java"))
        .andExpect(jsonPath("$.items[1].details.new").value("sql"))
        .andExpect(jsonPath("$.items[0].action").value("ATTRIBUTE_REMOVED"))
        .andExpect(jsonPath("$.items[0].details.old").value("sql"));
  }

  @Test
  void an_edit_without_the_attributes_property_keeps_the_stored_pairs() throws Exception {
    save(ana, 1, "[" + PAIR + "]");

    save(ana, 2, null)
        .andExpect(jsonPath("$.version").value(2))
        .andExpect(jsonPath("$.attributes.length()").value(1));
  }

  @Test
  void r_004_03_a_refused_pair_stores_nothing_and_names_the_second_pair() throws Exception {
    save(ana, 1, "[" + PAIR + ",{\"key\":\"Kompetenca\",\"value\":\"sql\"}]")
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("attributes[1].key"))
        .andExpect(jsonPath("$.fieldErrors[0].code").value("validation.attribute_duplicate_key"));

    mvc.perform(get("/api/v1/groups/" + acme + "/contacts/" + contact).with(as(ana)))
        .andExpect(jsonPath("$.version").value(1))
        .andExpect(jsonPath("$.attributes.length()").value(0));
  }

  @Test
  void r_004_07_a_stale_version_is_refused_and_the_current_contact_is_sent() throws Exception {
    save(ana, 1, "[" + PAIR + "]");

    save(ana, 1, "[{\"key\":\"x\",\"value\":\"y\"}]")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.current.attributes[0].key").value("kompetenca"));
  }

  @Test
  void r_004_08_a_viewer_is_refused_and_sees_the_pairs() throws Exception {
    save(ana, 1, "[" + PAIR + "]");
    User viola = newUser("Viola Vidmar");
    jdbc.sql("INSERT INTO member (group_id, user_id, role) VALUES (:g, :u, :r)")
        .param("g", acme)
        .param("u", viola.id())
        .param("r", Role.VIEWER.name())
        .update();

    save(viola, 2, "[]").andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/groups/" + acme + "/contacts/" + contact).with(as(viola)))
        .andExpect(jsonPath("$.attributes[0].value").value("java"));
  }

  @Test
  void r_004_09_every_value_is_text() throws Exception {
    save(ana, 1, "[{\"key\":\"starost\",\"value\":\"42\"}]")
        .andExpect(jsonPath("$.attributes[0].value").value("42"));
  }
}
