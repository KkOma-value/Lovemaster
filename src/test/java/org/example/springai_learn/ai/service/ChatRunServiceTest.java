package org.example.springai_learn.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.springai_learn.ChatMemory.DatabaseChatMemory;
import org.example.springai_learn.auth.entity.ChatRun;
import org.example.springai_learn.auth.entity.ChatRunStatus;
import org.example.springai_learn.auth.repository.ChatRunEventRepository;
import org.example.springai_learn.auth.repository.ChatRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatRunServiceTest {

    private ChatRunRepository chatRunRepository;
    private ChatRunEventRepository chatRunEventRepository;
    private DatabaseChatMemory databaseChatMemory;
    private ObjectMapper objectMapper;
    private ChatRunService chatRunService;

    @BeforeEach
    void setUp() {
        chatRunRepository = mock(ChatRunRepository.class);
        chatRunEventRepository = mock(ChatRunEventRepository.class);
        databaseChatMemory = mock(DatabaseChatMemory.class);
        objectMapper = new ObjectMapper();
        chatRunService = new ChatRunService(chatRunRepository, chatRunEventRepository, databaseChatMemory, objectMapper);
    }

    @Test
    void createRun_shouldEnsureConversationAndPersistQueuedRun() {
        when(chatRunRepository.save(any(ChatRun.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatRun run = chatRunService.createRun("user-1", "coach", "chat-1", "hello", null);

        assertEquals(ChatRunStatus.QUEUED, run.getStatus());
        assertEquals("chat-1", run.getChatId());
        assertEquals("coach", run.getChatType());
        verify(databaseChatMemory).ensureConversationExists("user-1:coach:chat-1");
    }

    @Test
    void appendContent_shouldAccumulatePartialResponseAndSetRunning() {
        ChatRun run = ChatRun.builder()
                .id("run-1")
                .userId("user-1")
                .chatId("chat-1")
                .chatType("loveapp")
                .status(ChatRunStatus.QUEUED)
                .build();
        when(chatRunRepository.findById("run-1")).thenReturn(Optional.of(run));
        when(chatRunRepository.save(any(ChatRun.class))).thenAnswer(invocation -> invocation.getArgument(0));

        chatRunService.appendContent("run-1", "你好");
        chatRunService.appendContent("run-1", "世界");

        ArgumentCaptor<ChatRun> captor = ArgumentCaptor.forClass(ChatRun.class);
        verify(chatRunRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        ChatRun saved = captor.getValue();
        assertEquals(ChatRunStatus.RUNNING, saved.getStatus());
        assertEquals("你好世界", saved.getPartialResponse());
        assertEquals("content", saved.getLastEventType());
    }
}
