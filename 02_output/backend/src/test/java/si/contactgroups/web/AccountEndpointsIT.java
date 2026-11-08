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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import si.contactgroups.domain.User;
import si.contactgroups.security.AuthenticatedUser;
import si.contactgroups.security.VerifiedToken;
import si.contactgroups.service.UserService;
import si.contactgroups.support.ApplicationContextSupport;

/** US-001: the User endpoint and the Group list behind the verified e-mail (D-40). */
class AccountEndpointsIT extends ApplicationContextSupport {

  @Autowired private MockMvc mvc;
  @Autowired private UserService users;
  @Autowired private JdbcClient jdbc;

  private User newUser(String name) {
    String uid = "uid-" + UUID.randomUUID();
    return users.findOrCreate(new VerifiedToken(uid, uid + "@acme.com", true, name));
  }

  private static RequestPostProcessor as(User user, boolean verified) {
    AuthenticatedUser principal = new AuthenticatedUser(user.id(), user.email(), verified);
    return authentication(
        UsernamePasswordAuthenticationToken.authenticated(
            principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
  }

  private UUID groupOf(User user, String name, String role, boolean inTrash) {
    UUID group =
        jdbc.sql(
                "INSERT INTO contact_group (name, deleted_at) VALUES (:name, "
                    + (inTrash ? "now()" : "null")
                    + ") RETURNING id")
            .param("name", name)
            .query(UUID.class)
            .single();
    jdbc.sql("INSERT INTO member (group_id, user_id, role) VALUES (:g, :u, :r)")
        .param("g", group)
        .param("u", user.id())
        .param("r", role)
        .update();
    return group;
  }

  @Test
  void r_001_11_me_returns_the_user_with_the_verified_flag() throws Exception {
    User ana = newUser("Ana Novak");

    mvc.perform(get("/api/v1/me").with(as(ana, true)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(ana.id().toString()))
        .andExpect(jsonPath("$.email").value(ana.email()))
        .andExpect(jsonPath("$.displayName").value("Ana Novak"))
        .andExpect(jsonPath("$.emailVerified").value(true))
        .andExpect(jsonPath("$.createdAt").exists());
  }

  @Test
  void r_001_04_an_unverified_user_may_read_me() throws Exception {
    User ana = newUser("Ana Novak");

    mvc.perform(get("/api/v1/me").with(as(ana, false)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.emailVerified").value(false));
  }

  @Test
  void r_001_04_an_unverified_user_gets_no_group() throws Exception {
    User ana = newUser("Ana Novak");
    UUID acme = groupOf(ana, "Acme", "OWNER", false);

    for (var request :
        List.of(
            get("/api/v1/groups"),
            post("/api/v1/groups").contentType(MediaType.APPLICATION_JSON).content("{}"),
            get("/api/v1/groups/" + acme + "/contacts"),
            get("/api/v1/group-trash"))) {
      mvc.perform(request.with(as(ana, false)))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("auth.email_not_verified"));
    }
  }

  @Test
  void r_001_03_a_verified_user_gets_the_groups_with_the_role() throws Exception {
    User ana = newUser("Ana Novak");
    User boris = newUser("Boris Kralj");
    UUID acme = groupOf(ana, "Acme", "OWNER", false);
    UUID club = groupOf(ana, "Club", "VIEWER", false);
    groupOf(ana, "Old", "OWNER", true);
    groupOf(boris, "Other", "OWNER", false);

    mvc.perform(get("/api/v1/groups").with(as(ana, true)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[?(@.id == '" + acme + "')].role").value("OWNER"))
        .andExpect(jsonPath("$[?(@.id == '" + club + "')].role").value("VIEWER"))
        .andExpect(jsonPath("$[?(@.id == '" + club + "')].name").value("Club"))
        .andExpect(jsonPath("$[0].createdAt").exists());
  }
}
