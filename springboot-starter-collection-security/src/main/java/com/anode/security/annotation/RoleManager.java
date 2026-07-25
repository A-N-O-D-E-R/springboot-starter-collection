package com.anode.security.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that the annotated method or type requires ROLE_MANAGER authority to access.
 * <p>
 * This annotation is a shorthand for {@code @PreAuthorize("hasRole('ROLE_MANAGER')")} and is used
 * for management-level operations that require elevated privileges but not full admin access.
 * When applied to a type, it applies to all methods within that type unless overridden by
 * method-level annotations.
 * <p>
 * Example usage:
 * <pre>{@code
 * @RoleManager
 * public void assignRole(String userId, String role) {
 *     // manager-level operation
 * }
 * }</pre>
 *
 * @see org.springframework.security.access.prepost.PreAuthorize
 * @see RoleAdmin
 * @see RoleOperator
 * @see RoleViewer
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ROLE_MANAGER')")
public @interface RoleManager {
}

