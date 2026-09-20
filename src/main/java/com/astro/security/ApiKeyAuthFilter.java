package com.astro.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Enterprise API Key Authentication & Sliding Rate-Limiting Filter.
 */
@Component
@Order(1)
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String ATTR_API_KEY = "CURRENT_API_KEY_DETAILS";
    private final ApiKeyService apiKeyService;
    private final ApiKeyRateLimiter rateLimiter;

    @Autowired
    public ApiKeyAuthFilter(ApiKeyService apiKeyService, ApiKeyRateLimiter rateLimiter) {
        this.apiKeyService = apiKeyService;
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!apiKeyService.isSecurityEnabled()) {
            return true; // Bypass security when disabled (e.g. offline unit testing)
        }

        String path = request.getRequestURI();
        // Whitelist public endpoints
        return path.startsWith("/actuator") ||
               path.startsWith("/api/billing/webhook") ||
               path.equals("/api/billing/plans") ||
               path.equals("/") ||
               path.endsWith(".html") ||
               path.endsWith(".js") ||
               path.endsWith(".css") ||
               path.endsWith(".ico") ||
               path.endsWith(".png");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String key = extractApiKey(request);

        if (key == null || key.isBlank()) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized: Missing API Key. Provide key via 'X-API-Key' header or '?apiKey=' query parameter.");
            return;
        }

        Optional<ApiKeyDetails> opt = apiKeyService.validateKey(key);
        if (opt.isEmpty()) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized: Invalid or deactivated API Key. Check subscription status at /api/billing/plans.");
            return;
        }

        ApiKeyDetails details = opt.get();

        // Check sliding window rate limit
        ApiKeyRateLimiter.RateLimitResult rateLimit = rateLimiter.checkRateLimit(details);
        response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimit.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(rateLimit.remaining()));

        if (!rateLimit.allowed()) {
            response.setHeader("Retry-After", String.valueOf(rateLimit.retryAfterSeconds()));
            sendError(response, 429,
                    "Too Many Requests: Rate limit of " + rateLimit.limit() + " req/min exceeded for tier "
                            + details.tier() + ". Retry in " + rateLimit.retryAfterSeconds() + " seconds.");
            return;
        }

        // Attach key details to request context
        request.setAttribute(ATTR_API_KEY, details);
        filterChain.doFilter(request, response);
    }

    private String extractApiKey(HttpServletRequest request) {
        // 1. Header: X-API-Key
        String key = request.getHeader("X-API-Key");
        if (key != null && !key.isBlank()) {
            return key.trim();
        }

        // 2. Header: Authorization: Bearer <key>
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.toLowerCase().startsWith("bearer ")) {
            return authHeader.substring(7).trim();
        }

        // 3. Query Parameter: ?apiKey=...
        String queryKey = request.getParameter("apiKey");
        if (queryKey != null && !queryKey.isBlank()) {
            return queryKey.trim();
        }

        return null;
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json; charset=utf-8");
        response.getWriter().write(String.format(
                "{\"error\": \"%s\", \"status\": %d, \"message\": \"%s\"}",
                status == 429 ? "Too Many Requests" : "Unauthorized",
                status,
                message
        ));
    }
}
