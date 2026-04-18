package org.example.springai_learn.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.auth.entity.WikiStrategyScore;
import org.example.springai_learn.auth.repository.WikiStrategyScoreRepository;
import org.example.springai_learn.config.KnowledgeProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Step 3: Provides top-K strategy ranking and gray-release determination.
 * This is a read-only service — it does NOT inject strategies into the retrieval path.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StrategyRankingService {

    private final WikiStrategyScoreRepository strategyScoreRepository;
    private final KnowledgeProperties properties;

    /**
     * Get top strategies for a given topic key, ordered by rank_score descending.
     */
    public List<WikiStrategyScore> topStrategies(String topicKey, int topK) {
        int limit = topK > 0 ? topK : properties.getStrategy().getTopK();
        if (topicKey != null && !topicKey.isBlank()) {
            return strategyScoreRepository.findByTopicKeyOrderByRankScoreDesc(topicKey, PageRequest.of(0, limit));
        }
        return strategyScoreRepository.findAllByOrderByRankScoreDesc(PageRequest.of(0, limit));
    }

    /**
     * Determine whether a strategy should be gray-enabled for a given user.
     * Uses consistent hashing (userId hashCode % 100) so the same user always gets
     * the same result for determinism.
     */
    public boolean shouldGrayEnable(String topicKey, String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        int grayPercent = properties.getStrategy().getGrayPercent();
        if (grayPercent <= 0) {
            return false;
        }
        if (grayPercent >= 100) {
            return true;
        }
        int hash = Math.abs(userId.hashCode());
        return (hash % 100) < grayPercent;
    }
}
