package org.example.springai_learn.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.ai.service.KnowledgeAbstractorService.StructuredKnowledge;
import org.example.springai_learn.ai.dto.KnowledgeCandidateRequest;
import org.example.springai_learn.ai.dto.KnowledgeCandidateResponse;
import org.example.springai_learn.auth.entity.WikiCandidate;
import org.example.springai_learn.auth.repository.WikiCandidateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeSinkService {

    private final WikiCandidateRepository wikiCandidateRepository;
    private final TopicSchemaService topicSchemaService;
    private final KnowledgeFeedbackService knowledgeFeedbackService;
    private final KnowledgeAbstractorService knowledgeAbstractorService;

    @Transactional
    public KnowledgeCandidateResponse createCandidate(String userId, KnowledgeCandidateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request 不能为空");
        }
        if (request.chatId() == null || request.chatId().isBlank()) {
            throw new IllegalArgumentException("chatId 不能为空");
        }
        if (request.answer() == null || request.answer().isBlank()) {
            throw new IllegalArgumentException("answer 不能为空");
        }

        TopicSchemaService.TopicClassification classification = topicSchemaService
                .classify(request.question(), request.answer());

        String candidateStatus = classification.unknownTopic() ? "unknown_topic" : "pending_review";
        WikiCandidate candidate = WikiCandidate.builder()
                .userId(userId)
                .sourceChatId(request.chatId())
                .sourceRunId(request.runId())
                .triggerType(normalizeTriggerType(request.triggerType()))
                .triggerScore(scaleScore(request.triggerScore()))
                .rawQuestion(request.question())
                .rawAnswer(request.answer())
                .stage(classification.stage())
                .intent(classification.intent())
                .problem(classification.problem())
                .schemaVersion(classification.schemaVersion())
                // Step2: 调用 LLM 蒸馏为结构化经验块（scenario/cause/strategy/taboo/summary）
                .abstractSummary(buildStructuredSummary(request.question(), request.answer()))
                .status(candidateStatus)
                .build();

        WikiCandidate saved = wikiCandidateRepository.save(candidate);
        knowledgeFeedbackService.recordCandidateSubmittedEvent(saved, classification.unknownTopic());
        log.info("Knowledge candidate saved: candidateId={}, chatId={}, userId={}, status={}, stage={}, intent={}, problem={}",
                saved.getId(),
                saved.getSourceChatId(),
                userId,
                saved.getStatus(),
                saved.getStage(),
                saved.getIntent(),
                saved.getProblem());

        return new KnowledgeCandidateResponse(
                saved.getId(),
                saved.getStatus(),
                saved.getStage(),
                saved.getIntent(),
                saved.getProblem(),
                saved.getSchemaVersion(),
                classification.unknownTopic(),
                saved.getCreatedAt()
        );
    }

    private BigDecimal scaleScore(Double score) {
        if (score == null) {
            return null;
        }
        return BigDecimal.valueOf(score).setScale(3, RoundingMode.HALF_UP);
    }

    private String normalizeTriggerType(String triggerType) {
        if (triggerType == null || triggerType.isBlank()) {
            return "manual";
        }
        return triggerType.trim();
    }

    private String buildStructuredSummary(String question, String answer) {
        try {
            StructuredKnowledge knowledge = knowledgeAbstractorService.summarize(question, answer);
            String json = knowledgeAbstractorService.toJson(knowledge);
            if (json != null && !json.isBlank()) {
                return json;
            }
        } catch (Exception ex) {
            log.warn("Structured abstraction failed, falling back to plain summary: {}", ex.getMessage());
        }
        return buildPlainSummary(question, answer);
    }

    private String buildPlainSummary(String question, String answer) {
        String q = trimTo(question, 120);
        String a = trimTo(answer, 320);
        if (q.isBlank()) {
            return "A: " + a;
        }
        return "Q: " + q + "\nA: " + a;
    }

    private String trimTo(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }
}
