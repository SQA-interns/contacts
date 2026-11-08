package si.contactgroups.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an endpoint that needs a valid token but has no Group in its path (the User, the Group
 * list, the Owner-only Group Trash). Such an endpoint checks its own rule in the service (ADR-05).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AccountEndpoint {}
