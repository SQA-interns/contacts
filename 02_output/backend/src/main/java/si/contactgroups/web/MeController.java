package si.contactgroups.web;

import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import si.contactgroups.domain.User;
import si.contactgroups.security.AccountEndpoint;
import si.contactgroups.security.AuthenticatedUser;
import si.contactgroups.service.AccountService;
import si.contactgroups.service.UserService;

/** The signed-in User (contract {@code getMe}; R-001-11) and the deletion of the account. */
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

  private final UserService users;
  private final AccountService accounts;

  public MeController(UserService users, AccountService accounts) {
    this.users = users;
    this.accounts = accounts;
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

  /** Contract {@code deleteMe}: 409 {@code account.sole_owner} lists the Groups (R-021-02). */
  @DeleteMapping
  @AccountEndpoint
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteMe(@AuthenticationPrincipal AuthenticatedUser caller) {
    accounts.deleteAccount(caller.userId());
  }
}
