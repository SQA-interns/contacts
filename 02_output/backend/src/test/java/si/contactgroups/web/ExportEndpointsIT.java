package si.contactgroups.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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

/** US-011 (T-35): the export endpoint, its Roles, its refusals and its History entry. */
class ExportEndpointsIT extends WebTestSupport {

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

  private User join(String name, Role role) {
    User user = newUser(name);
    jdbc.sql("INSERT INTO member (group_id, user_id, role) VALUES (:g, :u, :r)")
        .param("g", acme)
        .param("u", user.id())
        .param("r", role.name())
        .update();
    return user;
  }

  private String contact(String last) throws Exception {
    String json =
        mvc.perform(
                post("/api/v1/groups/" + acme + "/contacts")
                    .with(as(ana))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"lastName\":\"" + last + "\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(json, "$.id");
  }

  private ResultActions export(User user, String body) throws Exception {
    return mvc.perform(
        post("/api/v1/groups/" + acme + "/exports")
            .with(as(user))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private int entries() {
    return jdbc.sql(
            "SELECT count(*) FROM history_entry WHERE action = 'CONTACTS_EXPORTED'"
                + " AND group_id = :g")
        .param("g", acme)
        .query(Integer.class)
        .single();
  }

  @Test
  void aViewerIsRefusedAndNothingIsRecorded() throws Exception {
    String id = contact("Novak");
    User viola = join("Viola Vidmar", Role.VIEWER);

    export(viola, "{\"selection\":{\"contactIds\":[\"" + id + "\"]},\"lang\":\"en\"}")
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("auth.role_forbidden"));

    assertThat(entries()).isZero();
  }

  @Test
  void anEditorExportsAndTheEntryHoldsTheUserAndTheNumberOfContacts() throws Exception {
    String a = contact("Ena");
    String b = contact("Dva");
    User boris = join("Boris Kos", Role.EDITOR);

    String content =
        export(
                boris,
                "{\"selection\":{\"contactIds\":[\"" + a + "\",\"" + b + "\"]},\"lang\":\"en\"}")
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getHeader("Content-Disposition");

    assertThat(content).contains("Acme_");
    var entry =
        jdbc.sql(
                "SELECT user_name, CAST(details AS text) AS details FROM history_entry"
                    + " WHERE action = 'CONTACTS_EXPORTED' AND group_id = :g")
            .param("g", acme)
            .query()
            .singleRow();
    assertThat(entry.get("user_name")).isEqualTo("Boris Kos");
    assertThat(JsonPath.<Integer>read((String) entry.get("details"), "$.count")).isEqualTo(2);
  }

  @Test
  void aSelectionDeletedMeanwhileIsNotExportedAndNotCounted() throws Exception {
    String a = contact("Ena");
    String b = contact("Dva");
    jdbc.sql("UPDATE contact SET deleted_at = now() WHERE id = :id")
        .param("id", UUID.fromString(b))
        .update();

    export(ana, "{\"selection\":{\"contactIds\":[\"" + a + "\",\"" + b + "\"]}}")
        .andExpect(status().isOk());

    String details =
        jdbc.sql(
                "SELECT CAST(details AS text) FROM history_entry"
                    + " WHERE action = 'CONTACTS_EXPORTED' AND group_id = :g")
            .param("g", acme)
            .query(String.class)
            .single();
    assertThat(JsonPath.<Integer>read(details, "$.count")).isEqualTo(1);
  }

  @Test
  void moreThanTenThousandIsRefusedWithNoFileAndNoEntry() throws Exception {
    jdbc.sql(
            "INSERT INTO contact (group_id, created_by, last_name, last_name_norm)"
                + " SELECT :g, :u, 'N' || i, 'n' || i FROM generate_series(1, 10001) AS i")
        .param("g", acme)
        .param("u", ana.id())
        .update();

    export(ana, "{\"selection\":{\"filter\":{}},\"lang\":\"sl\"}")
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("export.too_many"));

    assertThat(entries()).isZero();
  }

  @Test
  void aBodyThatNamesNeitherIdsNorFilterOrBothIsMalformed() throws Exception {
    export(ana, "{\"selection\":{}}").andExpect(status().isBadRequest());
    export(ana, "{\"selection\":{\"contactIds\":[],\"filter\":{}}}")
        .andExpect(status().isBadRequest());
    export(ana, "{\"selection\":{\"contactIds\":[\"not-an-id\"]}}")
        .andExpect(status().isBadRequest());
    export(ana, "{\"selection\":{\"contactIds\":[]},\"lang\":\"de\"}")
        .andExpect(status().isBadRequest());
    assertThat(entries()).isZero();
  }
}
