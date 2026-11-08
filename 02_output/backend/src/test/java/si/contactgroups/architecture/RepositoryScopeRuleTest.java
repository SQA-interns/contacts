package si.contactgroups.architecture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import si.contactgroups.persistence.GlobalScope;

/**
 * The AR-02 rule must fail on a repository method without the Group id (a rule that cannot fail
 * proves nothing).
 */
class RepositoryScopeRuleTest {

  /** Violating fixture. */
  public static class BadRepository {
    public String findContact(UUID contactId) {
      return contactId.toString();
    }
  }

  /** Conforming fixture. */
  public static class GoodRepository {
    public String findContact(UUID groupId, UUID contactId) {
      return groupId + "/" + contactId;
    }
  }

  /** Conforming because it is marked global. */
  @GlobalScope
  public static class UserRepository {
    public String findByEmail(String email) {
      return email;
    }
  }

  private static JavaClasses classes(Class<?>... types) {
    return new ClassFileImporter().importClasses(types);
  }

  @Test
  void aMethodWithoutGroupIdIsAViolation() {
    assertThatThrownBy(
            () ->
                RepositoryScopeRule.rule("si.contactgroups.architecture..")
                    .check(classes(BadRepository.class)))
        .hasMessageContaining("does not take groupId first");
  }

  @Test
  void groupScopedAndGlobalRepositoriesPass() {
    assertThatCode(
            () ->
                RepositoryScopeRule.rule("si.contactgroups.architecture..")
                    .check(classes(GoodRepository.class, UserRepository.class)))
        .doesNotThrowAnyException();
  }
}
