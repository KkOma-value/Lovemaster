package org.example.springai_learn.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chat_runs")
public class ChatRun {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "chat_id", nullable = false, length = 36)
    private String chatId;

    @Column(name = "chat_type", nullable = false, length = 20)
    private String chatType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRunStatus status;

    @Column(name = "request_message", columnDefinition = "TEXT")
    private String requestMessage;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "last_event_type", length = 50)
    private String lastEventType;

    @Column(name = "latest_status_text", columnDefinition = "TEXT")
    private String latestStatusText;

    @Column(name = "partial_response", columnDefinition = "TEXT")
    private String partialResponse;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
        if (status == null) {
            status = ChatRunStatus.QUEUED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
