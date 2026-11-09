package si.contactgroups.service;

import java.time.Clock;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import si.contactgroups.domain.User;
import si.contactgroups.persistence.UserRepository;
import si.contactgroups.security.InvalidTokenException;
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
   * The User of a valid token, created on the first token with the e-mail in lowercase and the name
   * of the profile, or the e-mail when the profile has no name (R-001-11, R-001-02). The existence
   * of the Firebase account is not checked.
   *
   * @throws EmailTakenException when another Firebase identifier holds the e-mail (D-18d)
   */
  public User findOrCreate(VerifiedToken token) {
    return findOrCreate(token, uid -> true);
  }

  /**
   * As {@link #findOrCreate(VerifiedToken)}, but no User is created for a token of an account that
   * Firebase no longer holds: the ID token of a deleted account stays valid for up to an hour
   * (R-021-05). Firebase is asked only when the User is created, never for a stored User.
   *
   * @throws InvalidTokenException when the User is not stored and {@code accountExists} says no
   * @throws EmailTakenException when another Firebase identifier holds the e-mail (D-18d)
   */
  public User findOrCreate(VerifiedToken token, Predicate<String> accountExists) {
    return users
        .findByFirebaseUid(token.uid())
        .orElseGet(
            () -> {
              if (!accountExists.test(token.uid())) {
                throw new InvalidTokenException("account deleted", null);
              }
              String email = token.email().toLowerCase(Locale.ROOT);
              users.insertIfAbsent(token.uid(), email, displayName(token, email), clock.instant());
              return users.findByFirebaseUid(token.uid()).orElseThrow(EmailTakenException::new);
            });
  }

  /** The stored User; the caller comes from a resolved token, so the User exists. */
  public User get(UUID userId) {
    return users.findById(userId).orElseThrow();
  }

  private static String displayName(VerifiedToken token, String email) {
    String name = token.name();
    return name == null || name.isBlank() ? email : name.strip();
  }
}
