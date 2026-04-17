package org.example.springai_learn.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.ChatMemory.DatabaseChatMemory;
import org.example.springai_learn.ai.context.ConversationIds;
import org.example.springai_learn.ai.context.ProbabilityAnalysis;
import org.example.springai_learn.auth.entity.ChatRun;
import org.example.springai_learn.auth.entity.ChatRunEvent;
import org.example.springai_learn.auth.entity.ChatRunStatus;
import org.example.springai_learn.auth.repository.ChatRunEventRepository;
import org.example.springai_learn.auth.repository.ChatRunRepository;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class ChatRunService {

    private static final Set<ChatRunStatus> ACTIVE_STATUSES = Set.of(ChatRunStatus.QUEUED, ChatRunStatus.RUNNING);

    /**
     * 内部记录类 - 用于内存暂存待写入的状态
     */
    private record PendingStatus(String eventType, String statusText, LocalDateTime timestamp) {}

    /**
     * 内部记录类 - 用于内存暂存流式内容
     */
    private record PendingContent(StringBuilder content, LocalDateTime lastUpdate) {
        PendingContent() {
            this(new StringBuilder(), LocalDateTime.now());
        }
    }

    /**
     * 内存暂存 - runId -> 待写入的状态
     */
    private final ConcurrentHashMap<String, PendingStatus> pendingStatuses = new ConcurrentHashMap<>();

    /**
     * 内存暂存 - runId -> 待写入的流式内容
     */
    private final ConcurrentHashMap<String, PendingContent> pendingContents = new ConcurrentHashMap<>();

    private static final Set<String> STATUS_EVENT_TYPES = Set.of(
            "thinking",
            "status",
            "intake_status",
            "ocr_result",
            "rewrite_result",
            "rag_status",
            "probability_status",
            "probability_result",
            "tool_call"
    );

    private final ChatRunRepository chatRunRepository;
    private final ChatRunEventRepository chatRunEventRepository;
    private final DatabaseChatMemory databaseChatMemory;
    private final ObjectMapper objectMapper;

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

    /**
     * 记录概率分析完成事件。
     * 将完整概率分析结果序列化为 JSON 写入 chat_run_events 表，
     * 同时更新 chat_runs 表的 latestStatusText 和 lastEventType。
     *
     * @param runId 运行 ID
     * @param prob  概率分析结构化结果
     */
    @Transactional
    public void recordProbability(String runId, ProbabilityAnalysis prob) {
        if (prob == null) {
            return;
        }
        String summary = String.format("概率分析完成: %d%% (%s, %s置信度)",
                prob.probability(), prob.tier(), prob.confidence());
        // 1. 写入 chat_run_events 详细事件表
        try {
            String probJson = objectMapper.writeValueAsString(prob);
            ChatRunEvent event = ChatRunEvent.builder()
                    .runId(runId)
                    .eventType("probability_result")
                    .content(probJson)
                    .build();
            chatRunEventRepository.save(event);
            log.debug("概率分析事件已持久化到 chat_run_events: runId={}, prob={}%", runId, prob.probability());
        } catch (Exception e) {
            log.warn("保存概率分析事件到 chat_run_events 失败: runId={}, error={}", runId, e.getMessage());
        }
        // 2. 更新 chat_runs 行内状态（保持原有行为兼容）
        recordEvent(runId, "probability_result", summary);
    }

    /**
     * 异步记录事件 - 仅写入内存，不立即刷盘
     * 定时任务会周期性将暂存的数据写入数据库
     *
     * @param runId 运行ID
     * @param eventType 事件类型
     * @param content 事件内容
     */
    public void recordEventAsync(String runId, String eventType, String content) {
        pendingStatuses.put(runId, new PendingStatus(eventType, content, LocalDateTime.now()));
    }

    /**
     * 定时刷盘 - 每秒执行一次，将内存中的待写入状态和内容刷入数据库
     */
    @Scheduled(fixedRate = 1000)
    public void flushPendingStatuses() {
        if (pendingStatuses.isEmpty() && pendingContents.isEmpty()) {
            return;
        }
        // 刷盘状态
        pendingStatuses.forEach((runId, pending) -> {
            try {
                updateRun(runId, run -> {
                    if (run.getStartedAt() == null) {
                        run.setStartedAt(LocalDateTime.now());
                    }
                    if (run.getStatus() == ChatRunStatus.QUEUED) {
                        run.setStatus(ChatRunStatus.RUNNING);
                    }
                    run.setLastEventType(pending.eventType());
                    if (STATUS_EVENT_TYPES.contains(pending.eventType())) {
                        run.setLatestStatusText(pending.statusText());
                    }
                });
            } catch (Exception e) {
                log.warn("刷盘状态失败: runId={}, error={}", runId, e.getMessage());
            }
        });
        pendingStatuses.clear();
        // 刷盘内容（保留在内存，终态时一次性写入）
        // 注意：这里只更新 lastEventType，不写入 partialResponse
        pendingContents.forEach((runId, pending) -> {
            try {
                updateRun(runId, run -> {
                    if (run.getStartedAt() == null) {
                        run.setStartedAt(LocalDateTime.now());
                    }
                    if (run.getStatus() == ChatRunStatus.QUEUED) {
                        run.setStatus(ChatRunStatus.RUNNING);
                    }
                    run.setLastEventType("content");
                });
            } catch (Exception e) {
                log.warn("刷盘内容状态失败: runId={}, error={}", runId, e.getMessage());
            }
        });
        // pendingContents 不清空，终态时才清空
    }

    @Transactional
    public void appendContent(String runId, String chunk) {
        if (chunk == null || chunk.isBlank()) {
            return;
        }
        // 内存暂存，不立即写 DB
        pendingContents.compute(runId, (k, existing) -> {
            if (existing == null) {
                return new PendingContent(new StringBuilder(chunk), LocalDateTime.now());
            }
            existing.content().append(chunk);
            return new PendingContent(existing.content(), LocalDateTime.now());
        });
    }

    @Transactional
    public void markCompleted(String runId, String finalContent) {
        updateRun(runId, run -> {
            // 优先使用暂存的流式内容，否则使用传入的 finalContent
            String contentToSave = finalContent;
            PendingContent pending = pendingContents.get(runId);
            if (pending != null && pending.content().length() > 0) {
                contentToSave = pending.content().toString();
            }
            if (contentToSave != null && !contentToSave.isBlank()) {
                run.setPartialResponse(contentToSave);
            }
            if (run.getStartedAt() == null) {
                run.setStartedAt(LocalDateTime.now());
            }
            run.setStatus(ChatRunStatus.COMPLETED);
            run.setLatestStatusText(null);
            run.setLastEventType("done");
            run.setFinishedAt(LocalDateTime.now());
        });
        // 清理暂存
        pendingContents.remove(runId);
        pendingStatuses.remove(runId);
    }

    @Transactional
    public void markFailed(String runId, String errorMessage) {
        updateRun(runId, run -> {
            // 失败时也尝试保存暂存内容（部分响应可能有用）
            PendingContent pending = pendingContents.get(runId);
            if (pending != null && pending.content().length() > 0) {
                run.setPartialResponse(pending.content().toString());
            }
            if (run.getStartedAt() == null) {
                run.setStartedAt(LocalDateTime.now());
            }
            run.setStatus(ChatRunStatus.FAILED);
            run.setErrorMessage(errorMessage);
            run.setLatestStatusText(errorMessage);
            run.setLastEventType("error");
            run.setFinishedAt(LocalDateTime.now());
        });
        // 清理暂存
        pendingContents.remove(runId);
        pendingStatuses.remove(runId);
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
