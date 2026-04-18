package org.example.springai_learn.ai.dto;

import java.util.Map;

public record KnowledgeFeedbackEventRequest(
        String candidateId,
        String chatId,
        String runId,
        String eventType,
        String eventValue,
        Double eventScore,
        Map<String, Object> meta
) {
}
