package si.contactgroups.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import si.contactgroups.domain.User;
import si.contactgroups.security.AuthenticatedUser;
import si.contactgroups.security.VerifiedToken;
import si.contactgroups.service.UserService;
import si.contactgroups.support.ApplicationContextSupport;

/** US-002: creating a Group and the Group list ({@code POST} and {@code GET /groups}). */
class GroupEndpointsIT extends ApplicationContextSupport {

  @Autowired private MockMvc mvc;
  @Autowired private UserService users;
  @Autowired private JdbcClient jdbc;

  private User newUser(String name) {
    String uid = "uid-" + UUID.randomUUID();
    return users.findOrCreate(new VerifiedToken(uid, uid + "@acme.com", true, name));
  }

  private static RequestPostProcessor as(User user) {
    AuthenticatedUser principal = new AuthenticatedUser(user.id(), user.email(), true);
    return authentication(
        UsernamePasswordAuthenticationToken.authenticated(
            principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
  }

  private ResultActions create(User user, String body) throws Exception {
    return mvc.perform(
        post("/api/v1/groups")
            .with(as(user))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  @Test
  void r_002_01_the_creator_is_owner_of_the_group_with_the_trimmed_name() throws Exception {
    User ana = newUser("Ana Novak");

    create(ana, "{\"name\":\"  Šola ČŽ  \"}")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value("Šola ČŽ"))
        .andExpect(jsonPath("$.role").value("OWNER"))
        .andExpect(jsonPath("$.createdAt").exists());

    mvc.perform(get("/api/v1/groups").with(as(ana)))
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("Šola ČŽ"))
        .andExpect(jsonPath("$[0].role").value("OWNER"));
  }

  @Test
  void r_002_07_two_owners_may_use_the_same_name_and_each_sees_only_their_own() throws Exception {
    User ana = newUser("Ana Novak");
    User boris = newUser("Boris Kos");

    create(ana, "{\"name\":\"Acme\"}").andExpect(status().isCreated());
    create(boris, "{\"name\":\"Acme\"}").andExpect(status().isCreated());

    mvc.perform(get("/api/v1/groups").with(as(boris))).andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void r_002_04_there_is_no_limit_on_the_number_of_groups() throws Exception {
    User ana = newUser("Ana Novak");

    for (int i = 1; i <= 30; i++) {
      create(ana, "{\"name\":\"Skupina " + i + "\"}").andExpect(status().isCreated());
    }

    mvc.perform(get("/api/v1/groups").with(as(ana))).andExpect(jsonPath("$.length()").value(30));
  }

  private void expectNameRefused(User user, String body) throws Exception {
    create(user, body)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("validation.failed"))
        .andExpect(jsonPath("$.fieldErrors.length()").value(1))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
        .andExpect(jsonPath("$.fieldErrors[0].code").value("validation.name_invalid"));
  }

  @Test
  void r_002_02_an_empty_blank_missing_or_too_long_name_creates_no_group() throws Exception {
    User ana = newUser("Ana Novak");

    expectNameRefused(ana, "{\"name\":\"\"}");
    expectNameRefused(ana, "{\"name\":\"     \"}");
    expectNameRefused(ana, "{\"name\":\"" + "a".repeat(101) + "\"}");
    expectNameRefused(ana, "{\"name\":null}");
    expectNameRefused(ana, "{}");

    mvc.perform(get("/api/v1/groups").with(as(ana))).andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void r_002_02_a_name_of_100_characters_after_trimming_is_accepted() throws Exception {
    User ana = newUser("Ana Novak");
    String name = "a".repeat(100);

    create(ana, "{\"name\":\"  " + name + "  \"}")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value(name));
  }

  private void expectNameTaken(User user, String name) throws Exception {
    create(user, "{\"name\":\"" + name + "\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("group.name_taken"));
  }

  @Test
  void r_002_03_an_owner_gets_no_second_group_with_the_same_name_ignoring_case() throws Exception {
    User ana = newUser("Ana Novak");
    create(ana, "{\"name\":\"Acme\"}").andExpect(status().isCreated());
    create(ana, "{\"name\":\"Šola\"}").andExpect(status().isCreated());

    expectNameTaken(ana, "ACME");
    expectNameTaken(ana, " acme ");
    expectNameTaken(ana, "ŠOLA");

    mvc.perform(get("/api/v1/groups").with(as(ana))).andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void r_002_03_a_group_in_trash_does_not_count() throws Exception {
    User ana = newUser("Ana Novak");
    create(ana, "{\"name\":\"Acme\"}").andExpect(status().isCreated());
    jdbc.sql(
            "UPDATE contact_group SET deleted_at = now() WHERE id IN"
                + " (SELECT group_id FROM member WHERE user_id = :u)")
        .param("u", ana.id())
        .update();

    create(ana, "{\"name\":\"Acme\"}").andExpect(status().isCreated());
  }

  @Test
  void r_002_03_a_group_of_which_the_user_is_not_owner_does_not_count() throws Exception {
    User ana = newUser("Ana Novak");
    User boris = newUser("Boris Kos");
    create(boris, "{\"name\":\"Acme\"}").andExpect(status().isCreated());
    jdbc.sql(
            "INSERT INTO member (group_id, user_id, role) SELECT group_id, :ana, 'EDITOR'"
                + " FROM member WHERE user_id = :boris")
        .param("ana", ana.id())
        .param("boris", boris.id())
        .update();

    create(ana, "{\"name\":\"Acme\"}").andExpect(status().isCreated());

    mvc.perform(get("/api/v1/groups").with(as(ana))).andExpect(jsonPath("$.length()").value(2));
  }
}
