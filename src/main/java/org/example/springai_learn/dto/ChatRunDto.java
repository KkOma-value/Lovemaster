package org.example.springai_learn.dto;

import org.example.springai_learn.auth.entity.ChatRun;

import java.time.LocalDateTime;

public record ChatRunDto(
        String runId,
        String chatId,
        String chatType,
        String status,
        String lastEventType,
        String latestStatusText,
        String partialResponse,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
    public static ChatRunDto from(ChatRun run) {
        return new ChatRunDto(
                run.getId(),
                run.getChatId(),
                run.getChatType(),
                run.getStatus().name(),
                run.getLastEventType(),
                run.getLatestStatusText(),
                run.getPartialResponse(),
                run.getErrorMessage(),
                run.getCreatedAt(),
                run.getStartedAt(),
                run.getFinishedAt()
        );
    }
}
