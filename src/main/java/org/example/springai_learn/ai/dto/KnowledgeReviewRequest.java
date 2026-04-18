package org.example.springai_learn.ai.dto;

public record KnowledgeReviewRequest(
        String reviewerId,
        String note,
        String reason
) {
}
