package org.example.springai_learn.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.ai.dto.KnowledgeFeedbackEventRequest;
import org.example.springai_learn.ai.dto.KnowledgeFeedbackEventResponse;
import org.example.springai_learn.auth.entity.WikiCandidate;
import org.example.springai_learn.auth.entity.WikiFeedbackEvent;
import org.example.springai_learn.auth.repository.WikiFeedbackEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeFeedbackService {

    private final WikiFeedbackEventRepository wikiFeedbackEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public KnowledgeFeedbackEventResponse createEvent(String userId, KnowledgeFeedbackEventRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request 不能为空");
        }
        if (request.chatId() == null || request.chatId().isBlank()) {
            throw new IllegalArgumentException("chatId 不能为空");
        }
        if (request.eventType() == null || request.eventType().isBlank()) {
            throw new IllegalArgumentException("eventType 不能为空");
        }

        WikiFeedbackEvent event = WikiFeedbackEvent.builder()
                .candidateId(blankToNull(request.candidateId()))
                .sourceChatId(request.chatId().trim())
                .sourceRunId(blankToNull(request.runId()))
                .userId(normalizeUserId(userId))
                .eventType(normalizeEventType(request.eventType()))
                .eventValue(blankToNull(request.eventValue()))
                .eventScore(scaleScore(request.eventScore()))
                .metaJson(serializeMeta(request.meta()))
                .build();

        WikiFeedbackEvent saved = wikiFeedbackEventRepository.save(event);
        return toResponse(saved);
    }

    @Transactional
    public void recordCandidateSubmittedEvent(WikiCandidate candidate, boolean unknownTopic) {
        if (candidate == null) {
            return;
        }
        try {
            WikiFeedbackEvent event = WikiFeedbackEvent.builder()
                    .candidateId(candidate.getId())
                    .sourceChatId(candidate.getSourceChatId())
                    .sourceRunId(candidate.getSourceRunId())
                    .userId(candidate.getUserId())
                    .eventType("candidate_submitted")
                    .eventValue(candidate.getStatus())
                    .eventScore(candidate.getTriggerScore())
                    .metaJson(serializeMeta(Map.of(
                            "schemaVersion", defaultString(candidate.getSchemaVersion()),
                            "unknownTopic", unknownTopic,
                            "stage", defaultString(candidate.getStage()),
                            "intent", defaultString(candidate.getIntent()),
                            "problem", defaultString(candidate.getProblem())
                    )))
                    .build();
            wikiFeedbackEventRepository.save(event);
        } catch (Exception ex) {
            log.warn("record candidate_submitted feedback failed: candidateId={}, error={}",
                    candidate.getId(), ex.getMessage());
        }
    }

    private KnowledgeFeedbackEventResponse toResponse(WikiFeedbackEvent event) {
        Double score = event.getEventScore() == null ? null : event.getEventScore().doubleValue();
        return new KnowledgeFeedbackEventResponse(
                event.getId(),
                event.getCandidateId(),
                event.getEventType(),
                event.getEventValue(),
                score,
                event.getCreatedAt()
        );
    }

    private String normalizeUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return "anonymous";
        }
        return userId.trim();
    }

    private String normalizeEventType(String eventType) {
        return eventType.trim().toLowerCase(Locale.ROOT);
    }

    private BigDecimal scaleScore(Double score) {
        if (score == null) {
            return null;
        }
        return BigDecimal.valueOf(score).setScale(3, RoundingMode.HALF_UP);
    }

    private String serializeMeta(Map<String, Object> meta) {
        if (meta == null || meta.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(meta);
        } catch (JsonProcessingException ex) {
            log.warn("serialize feedback meta failed: {}", ex.getMessage());
            return null;
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
