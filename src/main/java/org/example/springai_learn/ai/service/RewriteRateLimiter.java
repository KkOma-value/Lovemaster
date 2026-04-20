package org.example.springai_learn.ai.service;

import org.example.springai_learn.ai.config.RewriteProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单滑动窗口限流，窗口为 60s。
 * 按 userId 与 IP 双维度分桶，任一维度命中即拒绝。
 * 无需外部依赖。
 */
@Component
public class RewriteRateLimiter {

    private static final long WINDOW_MILLIS = 60_000L;

    private final Map<String, Deque<Long>> userBuckets = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> ipBuckets = new ConcurrentHashMap<>();
    private final RewriteProperties properties;

    public RewriteRateLimiter(RewriteProperties properties) {
        this.properties = properties;
    }

    public boolean tryAcquire(String userId, String ip) {
        long now = System.currentTimeMillis();
        int userLimit = Math.max(1, properties.rateLimit().perUserPerMinute());
        int ipLimit = Math.max(1, properties.rateLimit().perIpPerMinute());

        if (userId != null && !userId.isBlank()
                && !accept(userBuckets, userId, now, userLimit)) {
            return false;
        }
        if (ip != null && !ip.isBlank()
                && !accept(ipBuckets, ip, now, ipLimit)) {
            return false;
        }
        return true;
    }

    private boolean accept(Map<String, Deque<Long>> buckets, String key, long now, int limit) {
        Deque<Long> deque = buckets.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (deque) {
            long cutoff = now - WINDOW_MILLIS;
            while (!deque.isEmpty() && deque.peekFirst() < cutoff) {
                deque.pollFirst();
            }
            if (deque.size() >= limit) {
                return false;
            }
            deque.offerLast(now);
            return true;
        }
    }
}
