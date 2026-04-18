package org.example.springai_learn.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "wiki_candidate")
public class WikiCandidate {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "source_chat_id", nullable = false, length = 64)
    private String sourceChatId;

    @Column(name = "source_run_id", length = 64)
    private String sourceRunId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "trigger_type", nullable = false, length = 32)
    private String triggerType;

    @Column(name = "trigger_score", precision = 4, scale = 3)
    private BigDecimal triggerScore;

    @Column(name = "raw_question", columnDefinition = "TEXT")
    private String rawQuestion;

    @Column(name = "raw_answer", nullable = false, columnDefinition = "TEXT")
    private String rawAnswer;

    @Column(name = "stage", nullable = false, length = 32)
    private String stage;

    @Column(name = "intent", nullable = false, length = 32)
    private String intent;

    @Column(name = "problem", nullable = false, length = 32)
    private String problem;

    @Column(name = "schema_version", nullable = false, length = 16)
    private String schemaVersion;

    @Column(name = "abstract_summary", columnDefinition = "TEXT")
    private String abstractSummary;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "reviewer_id", length = 36)
    private String reviewerId;

    @Column(name = "rejected_reason", columnDefinition = "TEXT")
    private String rejectedReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
        if (status == null || status.isBlank()) {
            status = "pending_review";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
