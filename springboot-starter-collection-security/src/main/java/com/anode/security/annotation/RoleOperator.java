package com.anode.security.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that the annotated method or type requires ROLE_OPERATOR authority to access.
 * <p>
 * This annotation is a shorthand for {@code @PreAuthorize("hasRole('ROLE_OPERATOR')")} and is used
 * for operations that can be performed by operators without full administrative privileges.
 * When applied to a type, it applies to all methods within that type unless overridden by
 * method-level annotations.
 * <p>
 * Example usage:
 * <pre>{@code
 * @RoleOperator
 * public void restartService() {
 *     // operator-level operation
 * }
 * }</pre>
 *
 * @see org.springframework.security.access.prepost.PreAuthorize
 * @see RoleAdmin
 * @see RoleManager
 * @see RoleViewer
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ROLE_OPERATOR')")
public @interface RoleOperator {
}
