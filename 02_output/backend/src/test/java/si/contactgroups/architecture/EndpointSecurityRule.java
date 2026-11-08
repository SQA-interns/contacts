package si.contactgroups.architecture;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import si.contactgroups.security.AccountEndpoint;
import si.contactgroups.security.PublicEndpoint;
import si.contactgroups.security.RequiresAction;

/**
 * AR-06 / SR-01 / SR-02: every mapped handler states how it is secured: {@code @RequiresAction} (a
 * Group endpoint, the Group id comes from the path), {@code @AccountEndpoint} (token only) or
 * {@code @PublicEndpoint} (the health check only). A path with {@code {groupId}} needs
 * {@code @RequiresAction}.
 */
final class EndpointSecurityRule {

  private EndpointSecurityRule() {}

  static List<String> violations(Class<?> controller) {
    List<String> out = new ArrayList<>();
    for (Method method : controller.getDeclaredMethods()) {
      RequestMapping mapping =
          AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
      if (mapping == null) {
        continue;
      }
      String name = controller.getSimpleName() + "." + method.getName();
      int marks = 0;
      boolean requires = method.isAnnotationPresent(RequiresAction.class);
      if (requires) {
        marks++;
      }
      if (method.isAnnotationPresent(AccountEndpoint.class)) {
        marks++;
      }
      if (method.isAnnotationPresent(PublicEndpoint.class)) {
        marks++;
      }
      if (marks != 1) {
        out.add(
            name + " must have exactly one of @RequiresAction, @AccountEndpoint, @PublicEndpoint");
      }
      boolean groupPath =
          String.join(",", mapping.path()).contains("{groupId}")
              || String.join(",", mapping.value()).contains("{groupId}");
      if (groupPath && !requires) {
        out.add(name + " has {groupId} in its path but no @RequiresAction");
      }
    }
    return out;
  }
}
