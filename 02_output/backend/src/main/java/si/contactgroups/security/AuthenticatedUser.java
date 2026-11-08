package si.contactgroups.security;

import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The caller of a request: the User found or created for the verified token (ADR-04) and the
 * verified flag of that token. It carries no Role: the Role is read from the database on every
 * request (SR-03).
 */
public record AuthenticatedUser(UUID userId, String email, boolean emailVerified) {

  /** The caller of the request in progress; empty when no token was accepted. */
  public static Optional<AuthenticatedUser> current() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
      return Optional.of(user);
    }
    return Optional.empty();
  }
}
