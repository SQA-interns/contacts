package si.contactgroups.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import java.util.UUID;
import si.contactgroups.persistence.GlobalScope;

/**
 * AR-02: every public method of a repository that is not marked {@code @GlobalScope} takes the
 * Group id as its first parameter (a {@link UUID} parameter named {@code groupId}).
 */
final class RepositoryScopeRule {

  private RepositoryScopeRule() {}

  static ArchRule rule(String repositoryPackage) {
    DescribedPredicate<JavaClass> scoped =
        DescribedPredicate.describe(
            "not marked @GlobalScope", c -> !c.isAnnotatedWith(GlobalScope.class));
    return ArchRuleDefinition.methods()
        .that()
        .arePublic()
        .and()
        .areDeclaredInClassesThat()
        .resideInAPackage(repositoryPackage)
        .and()
        .areDeclaredInClassesThat(scoped)
        .and()
        .areNotAnnotatedWith(GlobalScope.class)
        .should(takeGroupIdFirst())
        .allowEmptyShould(true);
  }

  private static ArchCondition<JavaMethod> takeGroupIdFirst() {
    return new ArchCondition<>("take the Group id (UUID groupId) as first parameter") {
      @Override
      public void check(JavaMethod method, ConditionEvents events) {
        boolean ok =
            !method.getRawParameterTypes().isEmpty()
                && method.getRawParameterTypes().get(0).isEquivalentTo(UUID.class)
                && "groupId".equals(parameterName(method));
        if (!ok) {
          events.add(
              SimpleConditionEvent.violated(
                  method, method.getFullName() + " does not take groupId first (AR-02)"));
        }
      }
    };
  }

  private static String parameterName(JavaMethod method) {
    try {
      Class<?> owner = method.getOwner().reflect();
      for (java.lang.reflect.Method m : owner.getDeclaredMethods()) {
        if (m.getName().equals(method.getName())
            && m.getParameterCount() == method.getRawParameterTypes().size()) {
          return m.getParameters()[0].getName();
        }
      }
    } catch (RuntimeException | LinkageError e) {
      return "";
    }
    return "";
  }
}
