package org.example.springai_learn.ai.service;

import org.example.springai_learn.ai.dto.KnowledgeCandidateRequest;
import org.example.springai_learn.ai.dto.KnowledgeCandidateResponse;
import org.example.springai_learn.auth.entity.WikiCandidate;
import org.example.springai_learn.auth.repository.WikiCandidateRepository;
import org.example.springai_learn.config.KnowledgeProperties;
import org.example.springai_learn.ai.service.KnowledgeAbstractorService.StructuredKnowledge;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeSinkServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void createCandidate_shouldPersistAndReturnPendingReview() throws IOException {
        TopicSchemaService topicSchemaService = buildTopicSchemaService();
        WikiCandidateRepository repository = mock(WikiCandidateRepository.class);
        KnowledgeFeedbackService feedbackService = mock(KnowledgeFeedbackService.class);
        when(repository.save(any(WikiCandidate.class))).thenAnswer(invocation -> {
            WikiCandidate candidate = invocation.getArgument(0);
            candidate.setId("candidate-1");
            candidate.setCreatedAt(LocalDateTime.of(2026, 4, 18, 10, 0));
            return candidate;
        });

        KnowledgeAbstractorService abstractorService = mock(KnowledgeAbstractorService.class);
        StructuredKnowledge fakeKnowledge = new StructuredKnowledge("场景", "成因", "策略", "禁忌", "一句话总结");
        when(abstractorService.summarize(any(), any())).thenReturn(fakeKnowledge);
        when(abstractorService.toJson(any())).thenReturn("{\"scenario\":\"场景\"}");

        KnowledgeSinkService service = new KnowledgeSinkService(repository, topicSchemaService, feedbackService, abstractorService);
        KnowledgeCandidateRequest request = new KnowledgeCandidateRequest(
                "chat-1",
                "run-1",
                "我在冷淡期可以继续邀约吗",
                "她最近很冷淡，我担心邀约被拒绝",
                "kiko",
                0.9231
        );

        KnowledgeCandidateResponse response = service.createCandidate("user-1", request);

        assertEquals("candidate-1", response.candidateId());
        assertEquals("pending_review", response.status());
        assertEquals("冷淡期", response.stage());
        assertEquals("邀约", response.intent());
        assertEquals("冷淡", response.problem());
        verify(feedbackService).recordCandidateSubmittedEvent(any(WikiCandidate.class), any(Boolean.class));
    }

    @Test
    void createCandidate_shouldMarkUnknownTopic_whenClassificationMisses() throws IOException {
        TopicSchemaService topicSchemaService = buildTopicSchemaService();
        WikiCandidateRepository repository = mock(WikiCandidateRepository.class);
        KnowledgeFeedbackService feedbackService = mock(KnowledgeFeedbackService.class);
        when(repository.save(any(WikiCandidate.class))).thenAnswer(invocation -> {
            WikiCandidate candidate = invocation.getArgument(0);
            candidate.setId("candidate-2");
            candidate.setCreatedAt(LocalDateTime.of(2026, 4, 18, 10, 0));
            return candidate;
        });

        KnowledgeAbstractorService abstractorService = mock(KnowledgeAbstractorService.class);
        StructuredKnowledge fakeKnowledge = new StructuredKnowledge("场景", "成因", "策略", "禁忌", "一句话总结");
        when(abstractorService.summarize(any(), any())).thenReturn(fakeKnowledge);
        when(abstractorService.toJson(any())).thenReturn("{\"scenario\":\"场景\"}");

        KnowledgeSinkService service = new KnowledgeSinkService(repository, topicSchemaService, feedbackService, abstractorService);
        KnowledgeCandidateRequest request = new KnowledgeCandidateRequest(
                "chat-2",
                "run-2",
                "这里没有任何标签关键词",
                "回答里也没有命中词",
                null,
                null
        );

        KnowledgeCandidateResponse response = service.createCandidate("user-2", request);

        assertEquals("unknown_topic", response.status());
        assertTrue(response.unknownTopic());
        assertEquals("unknown", response.stage());
        assertEquals("unknown", response.intent());
        assertEquals("unknown", response.problem());
        verify(feedbackService).recordCandidateSubmittedEvent(any(WikiCandidate.class), any(Boolean.class));
    }

    private TopicSchemaService buildTopicSchemaService() throws IOException {
        Path schemaPath = tempDir.resolve("topic-schema.yml");
        Files.writeString(schemaPath, """
                version: v2-test
                stages:
                  - 破冰期
                  - 冷淡期
                intents:
                  - 聊天
                  - 邀约
                problems:
                  - 不回复
                  - 冷淡
                """, StandardCharsets.UTF_8);

        KnowledgeProperties properties = new KnowledgeProperties();
        properties.getSink().setTopicSchemaPath(schemaPath.toString());
        properties.getSink().setUnknownLabel("unknown");
        return new TopicSchemaService(properties);
    }
}
