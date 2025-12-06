package com.lebvest.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitConfig implements WebMvcConfigurer {

    // IP-based rate limiting: 100 requests per minute per IP
    private static final int IP_REQUESTS_PER_MINUTE = 100;
    
    // User-based rate limiting: 200 requests per minute per user
    private static final int USER_REQUESTS_PER_MINUTE = 200;

    private final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> userBuckets = new ConcurrentHashMap();

    @Bean
    public HandlerInterceptor rateLimitInterceptor() {
        return new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler) {
                String ipAddress = getClientIpAddress(request);
                String userId = getUserId(request);

                // IP-based rate limiting
                Bucket ipBucket = ipBuckets.computeIfAbsent(ipAddress, k -> createBucket(IP_REQUESTS_PER_MINUTE));
                if (!ipBucket.tryConsume(1)) {
                    response.setStatus(429);
                    response.setHeader("X-RateLimit-Limit", String.valueOf(IP_REQUESTS_PER_MINUTE));
                    response.setHeader("X-RateLimit-Remaining", "0");
                    response.setHeader("Retry-After", "60");
                    return false;
                }

                // User-based rate limiting (if authenticated)
                if (userId != null) {
                    Bucket userBucket = userBuckets.computeIfAbsent(userId, k -> createBucket(USER_REQUESTS_PER_MINUTE));
                    if (!userBucket.tryConsume(1)) {
                        response.setStatus(429);
                        response.setHeader("X-RateLimit-Limit", String.valueOf(USER_REQUESTS_PER_MINUTE));
                        response.setHeader("X-RateLimit-Remaining", "0");
                        response.setHeader("Retry-After", "60");
                        return false;
                    }
                }

                return true;
            }
        };
    }

    private Bucket createBucket(int requestsPerMinute) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(requestsPerMinute, Refill.intervally(requestsPerMinute, Duration.ofMinutes(1))))
                .build();
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private String getUserId(HttpServletRequest request) {
        // Extract user ID from security context or JWT token
        // This will be populated by the JWT filter
        Object userId = request.getAttribute("userId");
        return userId != null ? userId.toString() : null;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**", "/api/public/**");
    }
}

