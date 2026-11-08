package si.contactgroups.web;

import static org.assertj.core.api.Assertions.assertThat;
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
import si.contactgroups.domain.Normaliser;
import si.contactgroups.domain.Role;
import si.contactgroups.domain.User;
import si.contactgroups.support.WebTestSupport;

/** US-003 (T-14, T-15): create, read and edit a Contact over the REST interface. */
class ContactEndpointsIT extends WebTestSupport {

  @Autowired private JdbcClient jdbc;

  private User ana;
  private UUID acme;

  @BeforeEach
  void anaOwnsAcme() throws Exception {
    ana = newUser("Ana Novak");
    acme = groupOf(ana, "Acme");
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

  private void join(User user, Role role) {
    jdbc.sql("INSERT INTO member (group_id, user_id, role) VALUES (:g, :u, :r)")
        .param("g", acme)
        .param("u", user.id())
        .param("r", role.name())
        .update();
  }

  private ResultActions create(User user, String body) throws Exception {
    return mvc.perform(
        post("/api/v1/groups/" + acme + "/contacts")
            .with(as(user))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private ResultActions update(User user, Object id, String body) throws Exception {
    return mvc.perform(
        put("/api/v1/groups/" + acme + "/contacts/" + id)
            .with(as(user))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private ResultActions read(User user, Object id) throws Exception {
    return mvc.perform(get("/api/v1/groups/" + acme + "/contacts/" + id).with(as(user)));
  }

  private String createdId(String body) throws Exception {
    String json =
        create(ana, body)
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return com.jayway.jsonpath.JsonPath.read(json, "$.id");
  }

  private long count() {
    return jdbc.sql("SELECT count(*) FROM contact WHERE group_id = :g")
        .param("g", acme)
        .query(Long.class)
        .single();
  }

  @Test
  void r_003_01_create_stores_version_1_and_returns_all_fields_with_empty_tags_and_attributes()
      throws Exception {
    create(ana, "{\"firstName\":\"Ana\",\"email\":\"ANA@Acme.com\",\"country\":\"SI\"}")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.version").value(1))
        .andExpect(jsonPath("$.firstName").value("Ana"))
        .andExpect(jsonPath("$.email").value("ana@acme.com"))
        .andExpect(jsonPath("$.country").value("SI"))
        .andExpect(jsonPath("$.tags.length()").value(0))
        .andExpect(jsonPath("$.attributes.length()").value(0))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.updatedAt").exists());
  }

  @Test
  void r_003_03_a_refused_contact_stores_nothing_and_names_the_fields() throws Exception {
    create(ana, "{\"email\":\"ana@\",\"phone\":\"abc\"}")
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("validation.failed"))
        .andExpect(jsonPath("$.fieldErrors.length()").value(2));

    assertThat(count()).isZero();
  }

  @Test
  void adr_06_the_comparison_columns_follow_the_saved_texts() throws Exception {
    String id =
        createdId(
            "{\"firstName\":\"Čeh\",\"lastName\":\"Škerlj\",\"company\":\"Žagar\","
                + "\"email\":\"A@B.si\",\"city\":\"Škofja Loka\"}");

    var row =
        jdbc.sql(
                "SELECT first_name_norm, last_name_norm, company_norm, email_norm, search_norm"
                    + " FROM contact WHERE id = :id")
            .param("id", UUID.fromString(id))
            .query()
            .singleRow();
    assertThat(row.get("first_name_norm")).isEqualTo(Normaliser.normalise("Čeh"));
    assertThat(row.get("last_name_norm")).isEqualTo("skerlj");
    assertThat(row.get("company_norm")).isEqualTo("zagar");
    assertThat(row.get("email_norm")).isEqualTo("a@b.si");
    assertThat(row.get("search_norm")).isEqualTo("ceh skerlj a@b.si zagar skofja loka");
  }

  @Test
  void r_003_08_a_viewer_reads_all_fields_and_an_outsider_gets_404() throws Exception {
    User viola = newUser("Viola Vidmar");
    join(viola, Role.VIEWER);
    String id =
        createdId("{\"firstName\":\"Ana\",\"notes\":\"Udeleženka\",\"jobTitle\":\"Vodja\"}");

    read(viola, id)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notes").value("Udeleženka"))
        .andExpect(jsonPath("$.jobTitle").value("Vodja"));
    read(newUser("Boris Kos"), id)
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("group.not_found"));
  }

  @Test
  void r_003_08_an_unknown_contact_is_404_contact_not_found() throws Exception {
    read(ana, UUID.randomUUID())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("contact.not_found"));
    read(ana, "not-an-id")
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("contact.not_found"));
  }

  @Test
  void r_003_09_a_viewer_cannot_create_or_edit() throws Exception {
    User viola = newUser("Viola Vidmar");
    join(viola, Role.VIEWER);
    String id = createdId("{\"firstName\":\"Ana\"}");

    create(viola, "{\"firstName\":\"X\"}")
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("auth.role_forbidden"));
    update(viola, id, "{\"firstName\":\"X\",\"version\":1}").andExpect(status().isForbidden());

    assertThat(count()).isEqualTo(1);
  }

  @Test
  void r_003_06_a_save_with_the_current_version_raises_it_by_one_and_stores_a_version_row()
      throws Exception {
    String id = createdId("{\"firstName\":\"Ana\",\"company\":\"Acme d.o.o.\"}");

    update(ana, id, "{\"firstName\":\"Ana\",\"company\":\"Beta d.o.o.\",\"version\":1}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(2))
        .andExpect(jsonPath("$.company").value("Beta d.o.o."));

    long versions =
        jdbc.sql("SELECT count(*) FROM contact_version WHERE contact_id = :id")
            .param("id", UUID.fromString(id))
            .query(Long.class)
            .single();
    assertThat(versions).isEqualTo(2);
  }

  @Test
  void r_003_07_a_stale_version_is_refused_with_the_current_contact() throws Exception {
    String id = createdId("{\"firstName\":\"Ana\"}");
    update(ana, id, "{\"firstName\":\"Ana\",\"city\":\"Maribor\",\"version\":1}")
        .andExpect(status().isOk());

    update(ana, id, "{\"firstName\":\"Ana\",\"company\":\"Beta\",\"version\":1}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("contact.changed_by_someone_else"))
        .andExpect(jsonPath("$.current.version").value(2))
        .andExpect(jsonPath("$.current.city").value("Maribor"));

    read(ana, id)
        .andExpect(jsonPath("$.version").value(2))
        .andExpect(jsonPath("$.company").doesNotExist());
  }

  @Test
  void r_003_06_an_edit_without_a_version_is_a_bad_request() throws Exception {
    String id = createdId("{\"firstName\":\"Ana\"}");

    update(ana, id, "{\"firstName\":\"Ana\"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("request.malformed"));
  }

  @Test
  void r_003_11_a_save_that_changes_nothing_keeps_the_version_and_stores_no_version_row()
      throws Exception {
    String id = createdId("{\"firstName\":\"Ana\",\"email\":\"ana@acme.com\"}");

    update(ana, id, "{\"firstName\":\" Ana \",\"email\":\"ANA@acme.com\",\"version\":1}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(1));

    long versions =
        jdbc.sql("SELECT count(*) FROM contact_version WHERE contact_id = :id")
            .param("id", UUID.fromString(id))
            .query(Long.class)
            .single();
    assertThat(versions).isEqualTo(1);
  }

  @Test
  void r_003_10_two_contacts_may_share_an_email() throws Exception {
    createdId("{\"email\":\"ana@acme.com\"}");
    createdId("{\"email\":\"ana@acme.com\"}");

    assertThat(count()).isEqualTo(2);
  }

  @Test
  void br_01_a_contact_of_another_group_is_not_found_in_this_group() throws Exception {
    User boris = newUser("Boris Kos");
    UUID beta = groupOf(boris, "Beta");
    String id = createdId("{\"firstName\":\"Ana\"}");

    mvc.perform(get("/api/v1/groups/" + beta + "/contacts/" + id).with(as(boris)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("contact.not_found"));
  }
}
