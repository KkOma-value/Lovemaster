package org.example.springai_learn.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.springai_learn.ai.dto.KnowledgeFeedbackEventRequest;
import org.example.springai_learn.ai.dto.KnowledgeFeedbackEventResponse;
import org.example.springai_learn.auth.entity.WikiFeedbackEvent;
import org.example.springai_learn.auth.repository.WikiFeedbackEventRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeFeedbackServiceTest {

    @Test
    void createEvent_shouldPersistAndReturnResponse() {
        WikiFeedbackEventRepository repository = mock(WikiFeedbackEventRepository.class);
        when(repository.save(any(WikiFeedbackEvent.class))).thenAnswer(invocation -> {
            WikiFeedbackEvent event = invocation.getArgument(0);
            event.setId("event-1");
            event.setCreatedAt(LocalDateTime.of(2026, 4, 18, 10, 30));
            return event;
        });

        KnowledgeFeedbackService service = new KnowledgeFeedbackService(repository, new ObjectMapper());
        KnowledgeFeedbackEventRequest request = new KnowledgeFeedbackEventRequest(
                "candidate-1",
                "chat-1",
                "run-1",
                "THUMBS_UP",
                "good",
                0.9385,
                Map.of("continued_chat", true, "response_latency", 12)
        );

        KnowledgeFeedbackEventResponse response = service.createEvent("user-1", request);

        assertEquals("event-1", response.eventId());
        assertEquals("candidate-1", response.candidateId());
        assertEquals("thumbs_up", response.eventType());
        assertEquals("good", response.eventValue());
        assertEquals(0.939, response.eventScore());
        assertEquals(LocalDateTime.of(2026, 4, 18, 10, 30), response.createdAt());
    }

    @Test
    void createEvent_shouldRejectWhenChatIdMissing() {
        WikiFeedbackEventRepository repository = mock(WikiFeedbackEventRepository.class);
        KnowledgeFeedbackService service = new KnowledgeFeedbackService(repository, new ObjectMapper());
        KnowledgeFeedbackEventRequest request = new KnowledgeFeedbackEventRequest(
                null,
                "  ",
                null,
                "thumbs_up",
                null,
                null,
                null
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createEvent("user-1", request)
        );

        assertTrue(exception.getMessage().contains("chatId"));
    }
}
