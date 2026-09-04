package com.urlshortener.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;

    @Value("${app.rate-limit.capacity}")
    private int capacity;

    @Value("${app.rate-limit.refill-duration-seconds}")
    private int windowSeconds;

    /**
     * Returns true if the request is allowed, false if the client has exceeded
     * the rate limit for the current time window.
     *
     * Uses Redis INCR (atomic increment) so concurrent requests from the same
     * client can't race past the limit — same atomicity principle as the
     * click-count increment in UrlRepository.
     */
    public boolean isAllowed(String clientKey) {
        String redisKey = "ratelimit:" + clientKey;

        Long currentCount = redisTemplate.opsForValue().increment(redisKey);

        if (currentCount != null && currentCount == 1L) {
            // First request in this window — set the expiry so the counter
            // automatically resets after windowSeconds.
            redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
        }

        return currentCount != null && currentCount <= capacity;
    }
}