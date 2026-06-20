package com.batch.treasury_management.security;

import com.batch.treasury_management.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ForcePasswordChangeFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();

            // Skip password change endpoint and auth endpoints
            String path = request.getRequestURI();
            if (path.contains("/change-password") ||
                    path.contains("/api/auth/") ||
                    path.contains("/swagger") ||
                    path.contains("/v3/api-docs")) {
                filterChain.doFilter(request, response);
                return;
            }

            userRepository.findByUsername(username).ifPresent(user -> {
                if (user.isFirstLogin()) {
                    try {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"success\":false,\"message\":\"Please change your password first using /api/auth/change-password\"}");
                    } catch (IOException e) {
                        // ignore
                    }
                }
            });
        }

        filterChain.doFilter(request, response);
    }
}
