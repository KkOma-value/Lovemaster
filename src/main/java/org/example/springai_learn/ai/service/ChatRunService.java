package org.example.springai_learn.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.ChatMemory.DatabaseChatMemory;
import org.example.springai_learn.ai.context.ConversationIds;
import org.example.springai_learn.auth.entity.ChatRun;
import org.example.springai_learn.auth.entity.ChatRunStatus;
import org.example.springai_learn.auth.repository.ChatRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRunService {

    private static final Set<ChatRunStatus> ACTIVE_STATUSES = Set.of(ChatRunStatus.QUEUED, ChatRunStatus.RUNNING);

    private static final Set<String> STATUS_EVENT_TYPES = Set.of(
            "thinking",
            "status",
            "intake_status",
            "ocr_result",
            "rewrite_result",
            "rag_status",
            "tool_call"
    );

    private final ChatRunRepository chatRunRepository;
    private final DatabaseChatMemory databaseChatMemory;

    @Transactional
    public ChatRun createRun(String userId, String chatType, String chatId, String requestMessage, String imageUrl) {
        databaseChatMemory.ensureConversationExists(ConversationIds.forType(userId, chatType, chatId));

        ChatRun run = ChatRun.builder()
                .userId(userId)
                .chatId(chatId)
                .chatType(chatType)
                .status(ChatRunStatus.QUEUED)
                .requestMessage(requestMessage)
                .imageUrl(imageUrl)
                .build();

        ChatRun saved = chatRunRepository.save(run);
        log.info("创建聊天运行: runId={}, chatId={}, chatType={}, userId={}",
                saved.getId(), chatId, chatType, userId);
        return saved;
    }

    @Transactional
    public void markRunning(String runId, String latestStatusText) {
        updateRun(runId, run -> {
            if (run.getStartedAt() == null) {
                run.setStartedAt(LocalDateTime.now());
            }
            run.setStatus(ChatRunStatus.RUNNING);
            run.setLatestStatusText(latestStatusText);
        });
    }

    @Transactional
    public void recordEvent(String runId, String eventType, String content) {
        updateRun(runId, run -> {
            if (run.getStartedAt() == null) {
                run.setStartedAt(LocalDateTime.now());
            }
            if (run.getStatus() == ChatRunStatus.QUEUED) {
                run.setStatus(ChatRunStatus.RUNNING);
            }
            run.setLastEventType(eventType);
            if (STATUS_EVENT_TYPES.contains(eventType)) {
                run.setLatestStatusText(content);
            }
            if ("error".equals(eventType)) {
                run.setErrorMessage(content);
            }
        });
    }

    @Transactional
    public void appendContent(String runId, String chunk) {
        if (chunk == null || chunk.isBlank()) {
            return;
        }
        updateRun(runId, run -> {
            if (run.getStartedAt() == null) {
                run.setStartedAt(LocalDateTime.now());
            }
            if (run.getStatus() == ChatRunStatus.QUEUED) {
                run.setStatus(ChatRunStatus.RUNNING);
            }
            run.setLastEventType("content");
            String existing = run.getPartialResponse() == null ? "" : run.getPartialResponse();
            run.setPartialResponse(existing + chunk);
        });
    }

    @Transactional
    public void markCompleted(String runId, String finalContent) {
        updateRun(runId, run -> {
            if (finalContent != null && !finalContent.isBlank()) {
                run.setPartialResponse(finalContent);
            }
            if (run.getStartedAt() == null) {
                run.setStartedAt(LocalDateTime.now());
            }
            run.setStatus(ChatRunStatus.COMPLETED);
            run.setLatestStatusText(null);
            run.setLastEventType("done");
            run.setFinishedAt(LocalDateTime.now());
        });
    }

    @Transactional
    public void markFailed(String runId, String errorMessage) {
        updateRun(runId, run -> {
            if (run.getStartedAt() == null) {
                run.setStartedAt(LocalDateTime.now());
            }
            run.setStatus(ChatRunStatus.FAILED);
            run.setErrorMessage(errorMessage);
            run.setLatestStatusText(errorMessage);
            run.setLastEventType("error");
            run.setFinishedAt(LocalDateTime.now());
        });
    }

    @Transactional
    public void markFailedIfActive(String runId, String errorMessage) {
        chatRunRepository.findById(runId).ifPresent(run -> {
            if (ACTIVE_STATUSES.contains(run.getStatus())) {
                run.setStatus(ChatRunStatus.FAILED);
                run.setErrorMessage(errorMessage);
                run.setLatestStatusText(errorMessage);
                run.setLastEventType("error");
                run.setFinishedAt(LocalDateTime.now());
                chatRunRepository.save(run);
            }
        });
    }

    @Transactional(readOnly = true)
    public List<ChatRun> listActiveRunsForUser(String userId) {
        return chatRunRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(userId, ACTIVE_STATUSES);
    }

    @Transactional(readOnly = true)
    public Optional<ChatRun> getRunForUser(String runId, String userId) {
        return chatRunRepository.findByIdAndUserId(runId, userId);
    }

    @Transactional(readOnly = true)
    public Optional<ChatRun> getLatestRunForChat(String userId, String chatId, String chatType) {
        return chatRunRepository.findFirstByUserIdAndChatIdAndChatTypeOrderByCreatedAtDesc(userId, chatId, chatType);
    }

    private void updateRun(String runId, Consumer<ChatRun> updater) {
        chatRunRepository.findById(runId).ifPresentOrElse(run -> {
            updater.accept(run);
            chatRunRepository.save(run);
        }, () -> log.warn("更新聊天运行失败，run 不存在: runId={}", runId));
    }
}
