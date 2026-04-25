package org.example.springai_learn.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.auth.entity.WikiCandidate;
import org.example.springai_learn.auth.repository.WikiCandidateRepository;
import org.example.springai_learn.config.KnowledgeProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * v2.0 隐式信号驱动的全自动审批引擎。
 *
 * 不再依赖单维 thumbs_up 计数；改为聚合 8 维信号的加权分：
 *   approve  if  aggregatedScore >= minAggregatedScore  AND  negativeCount < negativeVeto
 *   reject   if  超过 staleDays 且无任何正向信号
 *   unknown  if  超过 unknownTopicDays 仍未达阈值
 *
 * 触发周期: 默认每 10 分钟。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KnowledgeAutoApprovalJob {

    private final KnowledgeProperties properties;
    private final WikiCandidateRepository candidateRepository;
    private final SignalAggregator signalAggregator;
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
            SignalAggregator.AggregatedScore signals = signalAggregator.score(candidate);

            // 规则 1: 加权分达标 + 负信号未触发否决 → 自动批准
            boolean scorePass = signals.aggregatedScore() >= config.getMinAggregatedScore();
            boolean vetoTriggered = signals.negativeCount() >= config.getNegativeVeto();

            if (scorePass && !vetoTriggered) {
                try {
                    reviewService.approve(candidate.getId(), "auto-approval",
                            String.format("signal-driven score=%.2f pos=%d neg=%d",
                                    signals.aggregatedScore(),
                                    signals.positiveCount(),
                                    signals.negativeCount()));
                    approved++;
                    log.info("Auto-approved candidate={}, score={}, pos={}, neg={}, total={}",
                            candidate.getId(),
                            signals.aggregatedScore(),
                            signals.positiveCount(),
                            signals.negativeCount(),
                            signals.totalEvents());
                } catch (Exception ex) {
                    log.warn("Auto-approval failed for candidate={}: {}", candidate.getId(), ex.getMessage());
                }
                continue;
            }

            // 规则 2: 冷数据自动清理 (超过 staleDays 且无正向信号)
            long daysSinceCreation = java.time.Duration.between(
                    candidate.getCreatedAt(), LocalDateTime.now()).toDays();
            if (daysSinceCreation >= config.getStaleDays() && signals.positiveCount() == 0) {
                try {
                    String reason = vetoTriggered
                            ? "stale-with-negative-veto-" + daysSinceCreation + "d"
                            : "stale-no-signal-" + daysSinceCreation + "d";
                    reviewService.reject(candidate.getId(), "auto-approval", reason);
                    rejected++;
                    log.info("Auto-rejected stale candidate={}, age={}d, reason={}",
                            candidate.getId(), daysSinceCreation, reason);
                } catch (Exception ex) {
                    log.warn("Auto-reject failed for candidate={}: {}", candidate.getId(), ex.getMessage());
                }
                continue;
            }

            // 规则 3: 长期未达阈值的候选标记为 unknown_topic
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
