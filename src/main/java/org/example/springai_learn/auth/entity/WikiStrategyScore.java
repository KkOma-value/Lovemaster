package org.example.springai_learn.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(name = "wiki_strategy_score", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"topic_key", "strategy_id"})
})
public class WikiStrategyScore {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "topic_key", nullable = false, length = 96)
    private String topicKey;

    @Column(name = "strategy_id", nullable = false, length = 64)
    private String strategyId;

    @Column(name = "sample_count", nullable = false)
    private int sampleCount;

    @Column(name = "positive_rate", precision = 5, scale = 4)
    private BigDecimal positiveRate;

    @Column(name = "continue_rate", precision = 5, scale = 4)
    private BigDecimal continueRate;

    @Column(name = "confidence", precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "rank_score", precision = 6, scale = 4)
    private BigDecimal rankScore;

    @Column(name = "gray_enabled", nullable = false)
    private boolean grayEnabled;

    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        if (computedAt == null) {
            computedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        computedAt = LocalDateTime.now();
    }
}
