package com.example.emailGenerator.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private final StringRedisTemplate redisTemplate;

    // Allow 5 requests per minute per IP
    private static final int MAX_REQUESTS = 5;
    private static final int TIME_WINDOW_SECONDS = 60;

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
            // If it's the first request, set the 60-second expiration timer
            redisTemplate.expire(redisKey, TIME_WINDOW_SECONDS, TimeUnit.SECONDS);
        }
        if (currentCount != null && currentCount > MAX_REQUESTS) {
            // Block the request and return HTTP 429 Too Many Requests
            response.setStatus(429);
            response.getWriter().write("Too many requests! Please wait a minute.");
            return false;
        }
        return true; // Allow the request to proceed
    }
}
