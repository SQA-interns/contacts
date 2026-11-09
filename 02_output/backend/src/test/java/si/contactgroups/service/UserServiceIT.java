package si.contactgroups.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import si.contactgroups.domain.User;
import si.contactgroups.persistence.UserRepository;
import si.contactgroups.security.InvalidTokenException;
import si.contactgroups.security.VerifiedToken;
import si.contactgroups.support.ApplicationContextSupport;

/** US-001: the User is created from the first valid token (R-001-11, R-001-02, D-18d). */
class UserServiceIT extends ApplicationContextSupport {

  @Autowired private UserService users;
  @Autowired private UserRepository stored;

  private static String uid() {
    return "uid-" + UUID.randomUUID();
  }

  private static String email(String local) {
    return local + "." + UUID.randomUUID() + "@Acme.COM";
  }

  @Test
  void r_001_11_first_token_creates_user_with_lowercase_email_and_display_name() {
    String uid = uid();
    String email = email("Ana");

    User user = users.findOrCreate(new VerifiedToken(uid, email, true, "Ana Novak"));

    assertThat(user.id()).isNotNull();
    assertThat(user.firebaseUid()).isEqualTo(uid);
    assertThat(user.email()).isEqualTo(email.toLowerCase(java.util.Locale.ROOT));
    assertThat(user.displayName()).isEqualTo("Ana Novak");
    assertThat(user.createdAt()).isNotNull();
  }

  @Test
  void r_001_11_a_known_token_returns_the_same_user() {
    VerifiedToken token = new VerifiedToken(uid(), email("ana"), true, "Ana Novak");

    User first = users.findOrCreate(token);
    User second = users.findOrCreate(token);

    assertThat(second).isEqualTo(first);
  }

  @Test
  void r_001_02_a_token_without_name_gives_the_email_as_display_name() {
    String email = email("janez");

    User user = users.findOrCreate(new VerifiedToken(uid(), email, true, null));

    assertThat(user.displayName()).isEqualTo(email.toLowerCase(java.util.Locale.ROOT));
  }

  @Test
  void r_001_02_a_blank_name_gives_the_email_as_display_name() {
    String email = email("janez");

    User user = users.findOrCreate(new VerifiedToken(uid(), email, true, "  "));

    assertThat(user.displayName()).isEqualTo(email.toLowerCase(java.util.Locale.ROOT));
  }

  @Test
  void d_18d_a_second_identifier_with_the_same_email_is_refused() {
    String email = email("ana");
    users.findOrCreate(new VerifiedToken(uid(), email, true, "Ana Novak"));

    assertThatThrownBy(() -> users.findOrCreate(new VerifiedToken(uid(), email, true, "Ana")))
        .isInstanceOf(EmailTakenException.class);
  }

  @Test
  void r_021_05_a_token_of_an_account_that_firebase_no_longer_holds_creates_no_user() {
    VerifiedToken token = new VerifiedToken(uid(), email("boris"), true, "Boris Kos");

    assertThatThrownBy(() -> users.findOrCreate(token, uid -> false))
        .isInstanceOf(InvalidTokenException.class);

    assertThat(stored.findByFirebaseUid(token.uid())).isEmpty();
  }

  @Test
  void r_021_05_an_existing_firebase_account_creates_the_user() {
    VerifiedToken token = new VerifiedToken(uid(), email("boris"), true, "Boris Kos");

    User user = users.findOrCreate(token, uid -> uid.equals(token.uid()));

    assertThat(stored.findByFirebaseUid(token.uid())).contains(user);
  }

  @Test
  void r_021_05_the_account_is_looked_up_only_when_the_user_is_created() {
    VerifiedToken token = new VerifiedToken(uid(), email("ana"), true, "Ana Novak");
    User first = users.findOrCreate(token);

    User again =
        users.findOrCreate(
            token,
            uid -> {
              throw new AssertionError("Firebase must not be asked for a known user");
            });

    assertThat(again).isEqualTo(first);
  }
}
