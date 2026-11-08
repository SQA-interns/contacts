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
}
