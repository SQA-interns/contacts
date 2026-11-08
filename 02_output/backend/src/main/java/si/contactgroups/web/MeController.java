package si.contactgroups.web;

import java.time.Instant;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import si.contactgroups.domain.User;
import si.contactgroups.security.AccountEndpoint;
import si.contactgroups.security.AuthenticatedUser;
import si.contactgroups.service.UserService;

/** The signed-in User (contract {@code getMe}; R-001-11). */
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

  private final UserService users;

  public MeController(UserService users) {
    this.users = users;
  }

  /** Contract schema {@code User}. */
  public record UserResponse(
      UUID id, String email, String displayName, boolean emailVerified, Instant createdAt) {}

  @GetMapping
  @AccountEndpoint
  public UserResponse getMe(@AuthenticationPrincipal AuthenticatedUser caller) {
    User user = users.get(caller.userId());
    return new UserResponse(
        user.id(), user.email(), user.displayName(), caller.emailVerified(), user.createdAt());
  }
}
