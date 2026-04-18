package org.example.springai_learn.ai.dto;

import java.time.LocalDateTime;

public record KnowledgeCandidateResponse(
        String candidateId,
        String status,
        String stage,
        String intent,
        String problem,
        String schemaVersion,
        boolean unknownTopic,
        LocalDateTime createdAt
) {
}
