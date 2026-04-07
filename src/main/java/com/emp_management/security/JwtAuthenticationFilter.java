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

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        // ✅ 1. Skip JWT validation for PUBLIC endpoints
        if (isPublicEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractBearerToken(request);

        // ✅ 2. If NO token → just continue (Spring will handle auth later)
        if (!StringUtils.hasText(token) ||
                "null".equalsIgnoreCase(token) ||
                "undefined".equalsIgnoreCase(token)) {

            filterChain.doFilter(request, response);
            return;
        }

        // ✅ 3. Validate token
        if (!jwtTokenProvider.validateToken(token)) {
            sendUnauthorized(response, "Invalid or expired JWT token");
            return;
        }

        // ✅ 4. Load user
        String empId = jwtTokenProvider.getEmployeeIdFromToken(token);

        User user = userRepository.findByEmployee_EmpId(empId).orElse(null);

        if (user == null) {
            sendUnauthorized(response, "User not found");
            return;
        }

        // ✅ 5. Check session invalidation
        if (!jwtTokenProvider.isTokenIssuedAfterPasswordChange(
                token, user.getLastPasswordChangeAt())) {
            sendUnauthorized(response, "Session expired. Please log in again.");
            return;
        }

        // ✅ 6. Check account status
        if (user.getStatus() != com.emp_management.shared.enums.EmployeeStatus.ACTIVE) {
            sendUnauthorized(response, "Account is disabled.");
            return;
        }

        // ✅ 7. Set authentication
        String role = jwtTokenProvider.getRoleFromToken(token);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        empId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    // ✅ Public endpoints matcher
    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/auth") ||
                path.startsWith("/api/password-reset") ||
                path.startsWith("/api/flash-news") ||
                path.startsWith("/api/wfh") ||
                path.startsWith("/api/announcements") ||
                path.startsWith("/debug");
    }

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

        response.getWriter().write(
                "{\"error\": \"" + message + "\"}"
        );
    }
}