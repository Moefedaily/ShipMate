package com.shipmate.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.lang.NonNull;
import org.springframework.core.env.Environment;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Environment environment;

    public RateLimitFilter(Environment environment) {
        this.environment = environment;
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(5)
                .refillIntervally(5, Duration.ofMinutes(1))
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private Bucket resolveBucket(String ip) {
        return buckets.computeIfAbsent(ip, k -> createBucket());
    }

    @Override
    protected void doFilterInternal(
           @NonNull HttpServletRequest request,
           @NonNull HttpServletResponse response,
           @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if (Arrays.asList(environment.getActiveProfiles()).contains("test")) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        if (path.startsWith("/api/auth/login")|| path.startsWith("/api/auth/forgot-password")) {

            String ip = request.getRemoteAddr();
            Bucket bucket = resolveBucket(ip);

            if (!bucket.tryConsume(1)) {
                response.setStatus(429);
                response.getWriter().write("Too many login attempts");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}