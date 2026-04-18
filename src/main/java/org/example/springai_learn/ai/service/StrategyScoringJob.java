package org.example.springai_learn.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.auth.entity.WikiCandidate;
import org.example.springai_learn.auth.entity.WikiStrategyScore;
import org.example.springai_learn.auth.repository.WikiCandidateRepository;
import org.example.springai_learn.auth.repository.WikiFeedbackEventRepository;
import org.example.springai_learn.auth.repository.WikiStrategyScoreRepository;
import org.example.springai_learn.config.KnowledgeProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Step 3: Periodically aggregates wiki_feedback_event data into wiki_strategy_score.
 * Computes positive_rate, continue_rate (estimated), confidence, and rank_score.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StrategyScoringJob {

    private final KnowledgeProperties properties;
    private final WikiFeedbackEventRepository feedbackEventRepository;
    private final WikiCandidateRepository candidateRepository;
    private final WikiStrategyScoreRepository strategyScoreRepository;

    @Scheduled(cron = "${app.knowledge.strategy.cron:0 30 */6 * * *}")
    @Transactional
    public void computeScores() {
        KnowledgeProperties.Strategy config = properties.getStrategy();
        if (!config.isEnabled()) {
            log.info("Strategy scoring disabled, skipping");
            return;
        }

        LocalDateTime since = LocalDateTime.now().minusDays(config.getLookbackDays());
        List<Object[]> aggregated = feedbackEventRepository.aggregateByCandidateAndType(since);

        if (aggregated.isEmpty()) {
            log.info("Strategy scoring: no feedback events in lookback window ({}d)", config.getLookbackDays());
            return;
        }

        // Group by candidateId -> { eventType -> count }
        Map<String, Map<String, Long>> grouped = new HashMap<>();
        for (Object[] row : aggregated) {
            String candidateId = (String) row[0];
            String eventType = (String) row[1];
            long count = ((Number) row[2]).longValue();
            grouped.computeIfAbsent(candidateId, k -> new HashMap<>()).put(eventType, count);
        }

        int upserted = 0;
        int skipped = 0;

        for (Map.Entry<String, Map<String, Long>> entry : grouped.entrySet()) {
            String candidateId = entry.getKey();
            Map<String, Long> typeCounts = entry.getValue();

            long helpful = typeCounts.getOrDefault("helpful", 0L);
            long unhelpful = typeCounts.getOrDefault("unhelpful", 0L);
            long totalFeedback = helpful + unhelpful;

            if (totalFeedback < config.getMinSamples()) {
                skipped++;
                continue;
            }

            // Resolve topic key from candidate
            Optional<WikiCandidate> candidateOpt = candidateRepository.findById(candidateId);
            if (candidateOpt.isEmpty()) {
                skipped++;
                continue;
            }
            WikiCandidate candidate = candidateOpt.get();
            String topicKey = buildTopicKey(candidate.getStage(), candidate.getIntent(), candidate.getProblem());

            // Compute metrics
            double positiveRate = (double) helpful / totalFeedback;

            // continue_rate: use continue_chat events if available, otherwise estimate
            long continueChat = typeCounts.getOrDefault("continue_chat", 0L);
            long totalEvents = typeCounts.values().stream().mapToLong(Long::longValue).sum();
            double continueRate;
            if (continueChat > 0) {
                continueRate = (double) continueChat / totalEvents;
            } else {
                // Estimate: positive_rate * 0.8 (TODO: hook into actual continue_chat events)
                continueRate = positiveRate * 0.8;
            }

            double confidence = Math.min((double) totalFeedback / 30.0, 1.0);
            double rankScore = positiveRate * 0.6 + continueRate * 0.3 + confidence * 0.1;

            // UPSERT
            WikiStrategyScore score = strategyScoreRepository
                    .findByTopicKeyAndStrategyId(topicKey, candidateId)
                    .orElse(WikiStrategyScore.builder()
                            .topicKey(topicKey)
                            .strategyId(candidateId)
                            .build());

            score.setSampleCount((int) totalFeedback);
            score.setPositiveRate(toBigDecimal(positiveRate, 4));
            score.setContinueRate(toBigDecimal(continueRate, 4));
            score.setConfidence(toBigDecimal(confidence, 4));
            score.setRankScore(toBigDecimal(rankScore, 4));
            score.setGrayEnabled(false); // Gray enablement is handled by StrategyRankingService
            score.setComputedAt(LocalDateTime.now());

            strategyScoreRepository.save(score);
            upserted++;
        }

        log.info("Strategy scoring sweep done: upserted={}, skipped={}, totalCandidates={}",
                upserted, skipped, grouped.size());
    }

    static String buildTopicKey(String stage, String intent, String problem) {
        return safe(stage) + "|" + safe(intent) + "|" + safe(problem);
    }

    private static String safe(String v) {
        return v == null || v.isBlank() ? "unknown" : v.trim();
    }

    private static BigDecimal toBigDecimal(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
    }
}
