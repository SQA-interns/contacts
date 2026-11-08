package si.contactgroups.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import si.contactgroups.security.AuthenticatedUser;
import si.contactgroups.security.RequiresAction;
import si.contactgroups.service.GroupAccessService;
import si.contactgroups.service.GroupNotFoundException;

/**
 * The one place where a Group endpoint is authorised (ADR-05, SR-01): for a handler marked {@link
 * RequiresAction} it takes the Group id from the request path only (R-018-04) and lets {@link
 * GroupAccessService} check Membership (404) and Role (403) before the handler runs. A failure is
 * an exception that {@link ApiExceptionHandler} turns into the error format.
 */
public class GroupAccessInterceptor implements HandlerInterceptor {

  private static final String GROUP_ID = "groupId";

  private final GroupAccessService access;

  public GroupAccessInterceptor(GroupAccessService access) {
    this.access = access;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (handler instanceof HandlerMethod method) {
      RequiresAction required = method.getMethodAnnotation(RequiresAction.class);
      if (required != null) {
        access.requireAllowed(groupIdOf(request), caller().userId(), required.value());
      }
    }
    return true;
  }

  private static String groupIdOf(HttpServletRequest request) {
    Object variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
    if (variables instanceof Map<?, ?> map && map.get(GROUP_ID) instanceof String groupId) {
      return groupId;
    }
    throw new GroupNotFoundException();
  }

  private static AuthenticatedUser caller() {
    return AuthenticatedUser.current()
        .orElseThrow(
            () ->
                new IllegalStateException("a Group endpoint was reached without a signed-in User"));
  }
}
