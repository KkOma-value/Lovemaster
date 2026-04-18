package org.example.springai_learn.ai.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CandidateListItemResponse(
        String id,
        String stage,
        String intent,
        String problem,
        String abstractSummary,
        String triggerType,
        BigDecimal triggerScore,
        String status,
        LocalDateTime createdAt
) {
}
