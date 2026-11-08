package si.contactgroups.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import si.contactgroups.domain.Action;

/**
 * Marks a controller method that works on a Group. The authorisation interceptor reads the Group id
 * from the request path and checks Membership and Role for this action (ADR-05).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequiresAction {
  Action value();
}
