package org.example.springai_learn.ai.dto;

import java.time.LocalDateTime;

public record KnowledgeFeedbackEventResponse(
        String eventId,
        String candidateId,
        String eventType,
        String eventValue,
        Double eventScore,
        LocalDateTime createdAt
) {
}
