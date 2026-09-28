package com.urlshortener.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    // INCR and EXPIRE run as one atomic unit inside Redis, so a crash between
    // them can no longer leave a counter without a TTL (permanent block).
    private static final DefaultRedisScript<Long> INCR_WITH_TTL = new DefaultRedisScript<>(
            "local c = redis.call('INCR', KEYS[1]) " +
            "if c == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end " +
            "return c",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    @Value("${app.rate-limit.capacity}")
    private int capacity;

    @Value("${app.rate-limit.refill-duration-seconds}")
    private int windowSeconds;

    /**
     * Returns true if the request is allowed. Fail-open: if Redis is
     * unreachable, the request is allowed instead of taking the app down.
     */
    public boolean isAllowed(String clientKey) {
        try {
            Long count = redisTemplate.execute(
                    INCR_WITH_TTL,
                    List.of("ratelimit:" + clientKey),
                    String.valueOf(windowSeconds));
            return count != null && count <= capacity;
        } catch (Exception e) {
            log.warn("Rate limiter unavailable, allowing request: {}", e.getMessage());
            return true;
        }
    }
}