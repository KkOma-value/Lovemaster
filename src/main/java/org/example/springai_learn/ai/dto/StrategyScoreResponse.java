package org.example.springai_learn.ai.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StrategyScoreResponse(
        String id,
        String topicKey,
        String strategyId,
        int sampleCount,
        BigDecimal positiveRate,
        BigDecimal continueRate,
        BigDecimal confidence,
        BigDecimal rankScore,
        boolean grayEnabled,
        LocalDateTime computedAt
) {
}
