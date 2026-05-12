package com.gestion.qnt.security.filters;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitingFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, Bucket> loginBuckets    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> internalBuckets = new ConcurrentHashMap<>();

    private Bucket loginBucketFor(String ip) {
        return loginBuckets.computeIfAbsent(ip, k ->
            Bucket.builder()
                .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1))))
                .build());
    }

    private Bucket internalBucketFor(String ip) {
        return internalBuckets.computeIfAbsent(ip, k ->
            Bucket.builder()
                .addLimit(Bandwidth.classic(60, Refill.intervally(60, Duration.ofMinutes(1))))
                .build());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        boolean isLogin    = uri.endsWith("/auth/login");
        boolean isInternal = uri.contains("/internal/");

        if (!isLogin && !isInternal) {
            chain.doFilter(request, response);
            return;
        }

        String ip = resolveClientIp(request);
        Bucket bucket = isLogin ? loginBucketFor(ip) : internalBucketFor(ip);

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Demasiadas solicitudes. Intentá en un minuto.\"}");
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
