package org.example.springai_learn.ai.dto;

public record KnowledgeCandidateRequest(
        String chatId,
        String runId,
        String question,
        String answer,
        String triggerType,
        Double triggerScore
) {
}
