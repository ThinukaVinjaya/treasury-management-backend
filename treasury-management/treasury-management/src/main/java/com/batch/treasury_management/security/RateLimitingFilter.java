package com.batch.treasury_management.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, RequestLimit> requestLimits = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIP(request);
        RequestLimit limit = requestLimits.computeIfAbsent(clientIp, k -> new RequestLimit());

        if (limit.isAllowed()) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Too Many Requests. Please try again later.\"}");
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // Inner class to track requests per IP
    private static class RequestLimit {
        private int count = 0;
        private Instant resetTime = Instant.now().plus(Duration.ofMinutes(1));

        public boolean isAllowed() {
            if (Instant.now().isAfter(resetTime)) {
                count = 0;
                resetTime = Instant.now().plus(Duration.ofMinutes(1));
            }
            count++;
            return count <= 80; // 80 requests per minute per IP
        }
    }
}