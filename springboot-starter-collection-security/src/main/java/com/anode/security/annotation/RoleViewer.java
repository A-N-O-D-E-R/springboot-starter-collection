package com.anode.security.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that the annotated method or type requires ROLE_VIEWER authority to access.
 * <p>
 * This annotation is a shorthand for {@code @PreAuthorize("hasRole('ROLE_VIEWER')")} and is used
 * for read-only or view operations accessible to users with minimal privileges.
 * When applied to a type, it applies to all methods within that type unless overridden by
 * method-level annotations.
 * <p>
 * Example usage:
 * <pre>{@code
 * @RoleViewer
 * public Page<DataDto> getData(Pageable pageable) {
 *     // viewer-level operation
 * }
 * }</pre>
 *
 * @see org.springframework.security.access.prepost.PreAuthorize
 * @see RoleAdmin
 * @see RoleOperator
 * @see RoleManager
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ROLE_VIEWER')")
public @interface RoleViewer {
}
