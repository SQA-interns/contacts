package si.contactgroups.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import si.contactgroups.domain.User;
import si.contactgroups.security.AuthenticatedUser;
import si.contactgroups.security.VerifiedToken;
import si.contactgroups.service.UserService;

/** Base of the endpoint tests: a stored User and a request signed in as that User. */
public abstract class WebTestSupport extends ApplicationContextSupport {

  @Autowired protected MockMvc mvc;
  @Autowired private UserService users;

  protected User newUser(String name) {
    String uid = "uid-" + UUID.randomUUID();
    return users.findOrCreate(new VerifiedToken(uid, uid + "@acme.com", true, name));
  }

  protected static RequestPostProcessor as(User user, boolean verified) {
    AuthenticatedUser principal = new AuthenticatedUser(user.id(), user.email(), verified);
    return authentication(
        UsernamePasswordAuthenticationToken.authenticated(
            principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
  }

  protected static RequestPostProcessor as(User user) {
    return as(user, true);
  }
}
