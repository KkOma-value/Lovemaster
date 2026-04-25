package org.example.springai_learn.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.auth.entity.WikiCandidate;
import org.example.springai_learn.auth.entity.WikiFeedbackEvent;
import org.example.springai_learn.auth.repository.WikiFeedbackEventRepository;
import org.example.springai_learn.config.KnowledgeProperties;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * v2.0 隐式信号聚合器：把 8 维信号事件折算成单一加权分。
 *
 * 输出:
 *   - aggregatedScore: 所有事件按 signalWeights 表加权求和（负权重信号会扣分）
 *   - negativeCount  : 强负信号(thumbs_down)出现次数
 *   - positiveCount  : 正向事件总数
 *
 * 由 KnowledgeAutoApprovalJob 调用，替换旧的「≥3 thumbs_up」单维阈值。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SignalAggregator {

    private static final String NEGATIVE_VETO_EVENT = "thumbs_down";

    private final WikiFeedbackEventRepository feedbackEventRepository;
    private final KnowledgeProperties properties;

    public AggregatedScore score(WikiCandidate candidate) {
        if (candidate == null) {
            return AggregatedScore.empty();
        }
        List<WikiFeedbackEvent> events = feedbackEventRepository
                .findForAggregation(candidate.getId(), candidate.getSourceChatId());
        return aggregate(events);
    }

    AggregatedScore aggregate(List<WikiFeedbackEvent> events) {
        Map<String, Double> weights = properties.getAutoApproval().getSignalWeights();
        double score = 0.0;
        int negative = 0;
        int positive = 0;

        for (WikiFeedbackEvent event : events) {
            String type = event.getEventType() == null ? "" : event.getEventType().toLowerCase();
            Double weight = weights.get(type);
            if (weight == null) {
                continue;
            }
            score += weight;
            if (NEGATIVE_VETO_EVENT.equals(type)) {
                negative++;
            } else if (weight > 0) {
                positive++;
            }
        }

        return new AggregatedScore(score, positive, negative, events.size());
    }

    public record AggregatedScore(double aggregatedScore, int positiveCount, int negativeCount, int totalEvents) {
        public static AggregatedScore empty() {
            return new AggregatedScore(0.0, 0, 0, 0);
        }
    }
}
