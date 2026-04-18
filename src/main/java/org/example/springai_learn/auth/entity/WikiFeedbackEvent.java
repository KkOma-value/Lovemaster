package org.example.springai_learn.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "wiki_feedback_event")
public class WikiFeedbackEvent {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "candidate_id", length = 36)
    private String candidateId;

    @Column(name = "source_chat_id", nullable = false, length = 64)
    private String sourceChatId;

    @Column(name = "source_run_id", length = 64)
    private String sourceRunId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "event_type", nullable = false, length = 32)
    private String eventType;

    @Column(name = "event_value", length = 64)
    private String eventValue;

    @Column(name = "event_score", precision = 4, scale = 3)
    private BigDecimal eventScore;

    @Column(name = "meta_json", columnDefinition = "TEXT")
    private String metaJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed", nullable = false)
    private boolean processed;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
