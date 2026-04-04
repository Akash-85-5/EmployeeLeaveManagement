package com.emp_management.security;

import com.emp_management.feature.auth.entity.User;
import com.emp_management.feature.auth.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT filter (JWT-only, no refresh-token logic).
 *
 * Per-request checks:
 *  1. Extract Bearer token from Authorization header.
 *  2. Validate signature + expiry via JwtTokenProvider.
 *  3. Load user from DB (needed to read lastPasswordChangeAt).
 *  4. Reject token if issued BEFORE lastPasswordChangeAt
 *     → this is the session-invalidation mechanism after password reset.
 *  5. Reject if user account is not ACTIVE.
 *  6. Set SecurityContext so downstream @PreAuthorize works.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository   userRepository;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository   = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractBearerToken(request);

        if (StringUtils.hasText(token)) {

            // ── Step 1: Basic JWT validation (signature + expiry) ──────────
            if (!jwtTokenProvider.validateToken(token)) {
                sendUnauthorized(response, "Invalid or expired JWT token");
                return;
            }

            // ── Step 2: Load user ──────────────────────────────────────────
            String empId = jwtTokenProvider.getEmployeeIdFromToken(token);

            User user = userRepository.findByEmployee_EmpId(empId).orElse(null);

            if (user == null) {
                sendUnauthorized(response, "User not found");
                return;
            }

            // ── Step 3: Session invalidation check ────────────────────────
            // Reject tokens issued before the last password change
            if (!jwtTokenProvider.isTokenIssuedAfterPasswordChange(
                    token, user.getLastPasswordChangeAt())) {
                sendUnauthorized(response, "Session expired. Please log in again.");
                return;
            }

            // ── Step 4: Account active check ──────────────────────────────
            if (user.getStatus() != com.emp_management.shared.enums.EmployeeStatus.ACTIVE) {
                sendUnauthorized(response, "Account is disabled.");
                return;
            }

            // ── Step 5: Populate SecurityContext ──────────────────────────
            String role = jwtTokenProvider.getRoleFromToken(token);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            empId,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private void sendUnauthorized(HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}