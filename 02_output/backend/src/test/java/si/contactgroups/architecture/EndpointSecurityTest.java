package si.contactgroups.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import si.contactgroups.domain.Action;
import si.contactgroups.security.AccountEndpoint;
import si.contactgroups.security.RequiresAction;

/** AR-06 / SR-01: no endpoint exists that is not covered by the authorisation scheme. */
class EndpointSecurityTest {

  @RestController
  static class Unmarked {
    @GetMapping("/api/v1/unmarked")
    String handler() {
      return "x";
    }
  }

  @RestController
  static class GroupPathWithoutAction {
    @AccountEndpoint
    @PostMapping("/api/v1/groups/{groupId}/things")
    String handler() {
      return "x";
    }
  }

  @RestController
  static class Conforming {
    @RequiresAction(Action.READ_CONTACTS)
    @GetMapping("/api/v1/groups/{groupId}/contacts")
    String list() {
      return "x";
    }

    @AccountEndpoint
    @GetMapping("/api/v1/me")
    String me() {
      return "x";
    }
  }

  @Test
  void everyControllerOfTheApplicationIsSecured() {
    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
    List<String> violations = new ArrayList<>();
    for (BeanDefinition definition : scanner.findCandidateComponents("si.contactgroups.web")) {
      try {
        violations.addAll(
            EndpointSecurityRule.violations(Class.forName(definition.getBeanClassName())));
      } catch (ClassNotFoundException e) {
        throw new IllegalStateException(e);
      }
    }
    assertThat(violations).isEmpty();
  }

  @Test
  void ruleFlagsAnUnmarkedHandler() {
    assertThat(EndpointSecurityRule.violations(Unmarked.class)).hasSize(1);
  }

  @Test
  void ruleFlagsAGroupPathWithoutRequiresAction() {
    assertThat(EndpointSecurityRule.violations(GroupPathWithoutAction.class))
        .anyMatch(v -> v.contains("{groupId}"));
  }

  @Test
  void ruleAcceptsConformingHandlers() {
    assertThat(EndpointSecurityRule.violations(Conforming.class)).isEmpty();
  }
}
