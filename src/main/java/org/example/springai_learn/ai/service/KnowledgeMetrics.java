package org.example.springai_learn.ai.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * 知识检索链路的 Micrometer 指标汇聚入口。
 * 指标命名遵循 {namespace}_{subject}_{unit} 约定。
 */
@Component
public class KnowledgeMetrics {

    private static final String NS = "knowledge";

    private final MeterRegistry registry;
    private final ConcurrentMap<String, Timer> fanoutTimers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> hitCounters = new ConcurrentHashMap<>();
    private final Counter cacheHit;
    private final Counter cacheMiss;
    private final Counter dedupDropped;
    private final Counter sanitizerBlocked;
    private final Counter feedbackReinforced;
    private final Counter feedbackSkipped;

    public KnowledgeMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.cacheHit = Counter.builder(NS + "_cache_hits_total")
                .description("Knowledge retrieval cache hits").register(registry);
        this.cacheMiss = Counter.builder(NS + "_cache_misses_total")
                .description("Knowledge retrieval cache misses").register(registry);
        this.dedupDropped = Counter.builder(NS + "_dedup_dropped_total")
                .description("Segments dropped by embedding similarity dedup").register(registry);
        this.sanitizerBlocked = Counter.builder(NS + "_sanitizer_blocked_total")
                .description("Injection pattern hits redacted by sanitizer").register(registry);
        this.feedbackReinforced = Counter.builder(NS + "_feedback_reinforced_total")
                .description("Feedback events converted to wiki inbox files").register(registry);
        this.feedbackSkipped = Counter.builder(NS + "_feedback_skipped_total")
                .description("Feedback events skipped during reinforcement sweep").register(registry);
    }

    public void recordFanout(String strategy, long elapsedNanos) {
        String normalized = strategy == null ? "unknown" : strategy;
        Timer timer = fanoutTimers.computeIfAbsent(normalized, key -> Timer.builder(NS + "_fanout_seconds")
                .description("Knowledge fanout end-to-end latency")
                .tag("strategy", key)
                .register(registry));
        timer.record(elapsedNanos, TimeUnit.NANOSECONDS);
    }

    public void recordHit(String source) {
        String normalized = source == null ? "unknown" : source;
        Counter counter = hitCounters.computeIfAbsent(normalized, key -> Counter.builder(NS + "_hits_total")
                .description("Knowledge hits per source")
                .tag("source", key)
                .register(registry));
        counter.increment();
    }

    public void cacheHit() {
        cacheHit.increment();
    }

    public void cacheMiss() {
        cacheMiss.increment();
    }

    public void dedupDropped(int count) {
        if (count > 0) {
            dedupDropped.increment(count);
        }
    }

    public void sanitizerBlocked(int count) {
        if (count > 0) {
            sanitizerBlocked.increment(count);
        }
    }

    public void feedbackReinforced(int count) {
        if (count > 0) {
            feedbackReinforced.increment(count);
        }
    }

    public void feedbackSkipped(int count) {
        if (count > 0) {
            feedbackSkipped.increment(count);
        }
    }
}
