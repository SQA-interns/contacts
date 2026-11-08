package si.contactgroups.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import si.contactgroups.domain.User;
import si.contactgroups.security.VerifiedToken;
import si.contactgroups.support.ApplicationContextSupport;

/** US-001: the User is created from the first valid token (R-001-11, R-001-02, D-18d). */
class UserServiceIT extends ApplicationContextSupport {

  @Autowired private UserService users;

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
}
