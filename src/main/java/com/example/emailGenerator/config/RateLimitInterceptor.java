package com.example.emailGenerator.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private final StringRedisTemplate redisTemplate;

    @Value("${app.rate-limit.max-requests:5}")
    private int maxRequests;

    @Value("${app.rate-limit.time-window:60}")
    private int timeWindowSeconds;

    public RateLimitInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIp(request);
        String redisKey = "rate_limit:" + clientIp;
        // Increment the count for this IP
        Long currentCount = redisTemplate.opsForValue().increment(redisKey);
        if (currentCount != null && currentCount == 1) {
            // If it's the first request, set the expiration timer
            redisTemplate.expire(redisKey, timeWindowSeconds, TimeUnit.SECONDS);
        }
        if (currentCount != null) {
            // Add headers so the client/browser console can see the limit
            response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequests));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, maxRequests - currentCount)));
            
            if (currentCount > maxRequests) {
                // Throw an exception so the global exception handler can render the UI
                throw new com.example.emailGenerator.RateLimitExceededException("Too many requests! Please wait a minute.");
            }
        }
        return true; // Allow the request to proceed
    }
}
