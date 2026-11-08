package si.contactgroups.service;

import java.time.Clock;
import java.util.Locale;
import org.springframework.stereotype.Service;
import si.contactgroups.domain.User;
import si.contactgroups.persistence.UserRepository;
import si.contactgroups.security.VerifiedToken;

/** Knows the Users of the product (US-001). */
@Service
public class UserService {

  private final UserRepository users;
  private final Clock clock;

  public UserService(UserRepository users, Clock clock) {
    this.users = users;
    this.clock = clock;
  }

  /**
   * The User of a valid token, created on the first token with the e-mail in lowercase and the
   * name of the profile, or the e-mail when the profile has no name (R-001-11, R-001-02).
   *
   * @throws EmailTakenException when another Firebase identifier holds the e-mail (D-18d)
   */
  public User findOrCreate(VerifiedToken token) {
    return users
        .findByFirebaseUid(token.uid())
        .orElseGet(
            () -> {
              String email = token.email().toLowerCase(Locale.ROOT);
              users.insertIfAbsent(token.uid(), email, displayName(token, email), clock.instant());
              return users.findByFirebaseUid(token.uid()).orElseThrow(EmailTakenException::new);
            });
  }

  private static String displayName(VerifiedToken token, String email) {
    String name = token.name();
    return name == null || name.isBlank() ? email : name.strip();
  }
}
