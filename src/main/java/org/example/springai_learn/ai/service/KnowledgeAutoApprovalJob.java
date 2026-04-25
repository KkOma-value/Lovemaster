package org.example.springai_learn.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.auth.entity.WikiCandidate;
import org.example.springai_learn.auth.entity.WikiFeedbackEvent;
import org.example.springai_learn.auth.repository.WikiCandidateRepository;
import org.example.springai_learn.auth.repository.WikiFeedbackEventRepository;
import org.example.springai_learn.config.KnowledgeProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户反馈驱动的全自动审批引擎。
 *
 * 不再需要人工审批。当候选积累足够的正向用户反馈后自动批准写入 topics/，
 * 冷数据自动清理，彻底消除人工介入瓶颈。
 *
 * 触发周期: 默认每 10 分钟
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KnowledgeAutoApprovalJob {

    private final KnowledgeProperties properties;
    private final WikiCandidateRepository candidateRepository;
    private final WikiFeedbackEventRepository feedbackEventRepository;
    private final KnowledgeReviewService reviewService;
    private final KnowledgeMetrics metrics;

    @Scheduled(cron = "${app.knowledge.auto-approval.cron:0 */10 * * * *}")
    public void autoApproveFromFeedback() {
        KnowledgeProperties.AutoApproval config = properties.getAutoApproval();
        if (!config.isEnabled()) {
            return;
        }

        List<WikiCandidate> pending = candidateRepository.findAllByStatus("pending_review");
        if (pending.isEmpty()) {
            log.debug("Auto-approval: no pending candidates");
            return;
        }

        int approved = 0;
        int rejected = 0;
        int unknownTopic = 0;

        for (WikiCandidate candidate : pending) {
            // 查询该候选关联的所有反馈事件
            List<WikiFeedbackEvent> events = feedbackEventRepository
                    .findByCandidateId(candidate.getId());

            long positiveCount = events.stream()
                    .filter(e -> config.getPositiveEventTypes().contains(
                            e.getEventType() != null ? e.getEventType().toLowerCase() : ""))
                    .filter(e -> e.getEventScore() != null
                            && e.getEventScore().doubleValue() >= config.getPositiveScoreThreshold())
                    .count();

            long negativeCount = events.stream()
                    .filter(e -> config.getNegativeEventTypes().contains(
                            e.getEventType() != null ? e.getEventType().toLowerCase() : ""))
                    .count();

            // 规则 1: 足够正向反馈 → 自动批准
            if (positiveCount >= config.getMinPositiveFeedback()) {
                try {
                    reviewService.approve(candidate.getId(), "auto-approval", "feedback-driven");
                    approved++;
                    log.info("Auto-approved candidate={}, positiveFeedback={}, negativeFeedback={}",
                            candidate.getId(), positiveCount, negativeCount);
                } catch (Exception ex) {
                    log.warn("Auto-approval failed for candidate={}: {}", candidate.getId(), ex.getMessage());
                }
                continue;
            }

            // 规则 2: 冷数据自动清理 (超过 staleDays 且反馈不足)
            long daysSinceCreation = java.time.Duration.between(
                    candidate.getCreatedAt(), LocalDateTime.now()).toDays();
            if (daysSinceCreation >= config.getStaleDays() && positiveCount == 0) {
                try {
                    reviewService.reject(candidate.getId(), "auto-approval",
                            "stale-candidate-no-positive-feedback-" + daysSinceCreation + "d");
                    rejected++;
                    log.info("Auto-rejected stale candidate={}, age={}d", candidate.getId(), daysSinceCreation);
                } catch (Exception ex) {
                    log.warn("Auto-reject failed for candidate={}: {}", candidate.getId(), ex.getMessage());
                }
                continue;
            }

            // 规则 3: 长期未被处理的候选标记为 unknown_topic
            if (daysSinceCreation >= config.getUnknownTopicDays()) {
                candidate.setStatus("unknown_topic");
                candidateRepository.save(candidate);
                unknownTopic++;
                log.info("Marked candidate={} as unknown_topic, age={}d", candidate.getId(), daysSinceCreation);
            }
        }

        metrics.autoApproved(approved);
        metrics.autoRejected(rejected);
        metrics.markedUnknownTopic(unknownTopic);

        log.info("Auto-approval sweep done: approved={}, rejected={}, unknownTopic={}, pendingRemaining={}",
                approved, rejected, unknownTopic,
                pending.size() - approved - rejected - unknownTopic);
    }
}
