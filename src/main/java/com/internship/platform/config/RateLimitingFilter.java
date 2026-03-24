package com.internship.platform.config;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory rate limiter: max 10 requests per minute per IP
 * applied to sensitive endpoints (/auth/login, /auth/refresh).
 */
@Component
@WebFilter(urlPatterns = { "/api/auth/login", "/api/auth/refresh" })
public class RateLimitingFilter implements Filter {

    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_MS = 60_000;

    // key = IP, value = [count, windowStartMs]
    private final Map<String, long[]> ipCounters = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String uri = request.getRequestURI();
        if (!uri.contains("/auth/login") && !uri.contains("/auth/refresh")) {
            chain.doFilter(req, res);
            return;
        }

        String ip = getClientIp(request);
        long now = Instant.now().toEpochMilli();
        long[] counter = ipCounters.compute(ip, (k, v) -> {
            if (v == null || now - v[1] > WINDOW_MS)
                return new long[] { 1, now };
            v[0]++;
            return v;
        });

        if (counter[0] > MAX_REQUESTS) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Trop de tentatives. Réessayez dans une minute.\"}");
            return;
        }
        chain.doFilter(req, res);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isEmpty()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }
}
