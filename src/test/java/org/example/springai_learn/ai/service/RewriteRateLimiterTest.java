package org.example.springai_learn.ai.service;

import org.example.springai_learn.ai.config.RewriteProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RewriteRateLimiterTest {

    @Test
    void tryAcquire_shouldBlockAfterLimitReached_forSameUserAndIp() {
        RewriteProperties properties = new RewriteProperties(false, new RewriteProperties.RateLimit(2, 2));
        RewriteRateLimiter limiter = new RewriteRateLimiter(properties);

        assertTrue(limiter.tryAcquire("user-1", "127.0.0.1"));
        assertTrue(limiter.tryAcquire("user-1", "127.0.0.1"));
        assertFalse(limiter.tryAcquire("user-1", "127.0.0.1"));
    }

    @Test
    void tryAcquire_shouldAllowDifferentUsers_whenOneUserHitsLimit() {
        RewriteProperties properties = new RewriteProperties(false, new RewriteProperties.RateLimit(1, 10));
        RewriteRateLimiter limiter = new RewriteRateLimiter(properties);

        assertTrue(limiter.tryAcquire("user-1", "127.0.0.1"));
        assertFalse(limiter.tryAcquire("user-1", "127.0.0.2"));
        assertTrue(limiter.tryAcquire("user-2", "127.0.0.2"));
    }
}
