package si.contactgroups.persistence;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a repository (or method) whose data is not scoped to one Group (Users, the Group list of a
 * User, the purge job). Every other repository method takes the Group id as its first parameter
 * (AR-02, ADR-01).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface GlobalScope {}
