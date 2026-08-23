package com.aiincidentcommander.api_gateway.ratelimit;

import com.aiincidentcommander.api_gateway.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;


@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final JwtService jwtService;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final List<String> EXCLUDED_PATTERNS = List.of(
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    );

    private static final List<String> AUTH_TIER_PATTERNS = List.of(
            "/api/auth/**"
    );

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (matchesAny(EXCLUDED_PATTERNS, path)) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitTier tier = matchesAny(AUTH_TIER_PATTERNS, path) ? RateLimitTier.AUTH : RateLimitTier.GENERAL;
        String clientKey = resolveClientKey(request, tier);

        Bucket bucket = rateLimitService.resolveBucket(tier, clientKey);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimitService.capacityFor(tier)));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(probe.getRemainingTokens(), 0)));
        response.setHeader("X-RateLimit-Reset",
                String.valueOf(Instant.now().plusNanos(probe.getNanosToWaitForRefill()).getEpochSecond()));

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
        } else {
            long waitSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000);
            response.setStatus(429); // 429 Too Many Requests
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(waitSeconds));
            response.getWriter().write(MAPPER.writeValueAsString(Map.of(
                    "status", 429,
                    "error", "Too Many Requests",
                    "message", "Rate limit exceeded. Try again in " + waitSeconds + "s.",
                    "path", path
            )));
        }
    }

    private String resolveClientKey(HttpServletRequest request, RateLimitTier tier) {
        if (tier == RateLimitTier.GENERAL) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String username = jwtService.extractUsername(authHeader.substring(7));
                    if (username != null && !username.isBlank()) {
                        return "user:" + username;
                    }
                } catch (JwtException | IllegalArgumentException ignored) {

                }
            }
        }
        return "ip:" + resolveIp(request);
    }

    private String resolveIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean matchesAny(List<String> patterns, String path) {
        return patterns.stream().anyMatch(p -> PATH_MATCHER.match(p, path));
    }
}