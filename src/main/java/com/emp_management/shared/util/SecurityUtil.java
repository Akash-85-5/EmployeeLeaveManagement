// ─────────────────────────────────────────────────────────────────────────────
// FILE: com/emp_management/shared/util/SecurityUtil.java
// ─────────────────────────────────────────────────────────────────────────────
package com.emp_management.shared.util;

import com.emp_management.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility to extract the current user's ID from the Spring Security context.
 *
 * Assumption: your JWT / UserDetails stores the employee ID as the principal name.
 * Adjust the cast to match your actual UserDetails implementation.
 */
public class SecurityUtil {

    public static String getCurrentUserId() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails user)) {
            return null;
        }

        return user.getEmpId();
    }
}