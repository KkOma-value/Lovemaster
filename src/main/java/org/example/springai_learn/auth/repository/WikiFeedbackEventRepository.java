package org.example.springai_learn.auth.repository;

import org.example.springai_learn.auth.entity.WikiFeedbackEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface WikiFeedbackEventRepository extends JpaRepository<WikiFeedbackEvent, String> {

    @Query("SELECT e FROM WikiFeedbackEvent e " +
            "WHERE e.processed = false " +
            "  AND e.candidateId IS NOT NULL " +
            "  AND e.eventScore IS NOT NULL " +
            "  AND e.eventScore >= :minScore " +
            "  AND LOWER(e.eventType) IN :eventTypes " +
            "ORDER BY e.eventScore DESC, e.createdAt ASC")
    List<WikiFeedbackEvent> findUnprocessedHighScoreEvents(
            @Param("minScore") BigDecimal minScore,
            @Param("eventTypes") List<String> eventTypes,
            Pageable pageable
    );

    /**
     * Step 3: Aggregate feedback events by candidateId within a time window.
     * Returns [candidateId, eventType, count] tuples for strategy scoring.
     */
    @Query("SELECT e.candidateId, e.eventType, COUNT(e) " +
            "FROM WikiFeedbackEvent e " +
            "WHERE e.createdAt >= :since " +
            "  AND e.candidateId IS NOT NULL " +
            "GROUP BY e.candidateId, e.eventType")
    List<Object[]> aggregateByCandidateAndType(@Param("since") LocalDateTime since);

    List<WikiFeedbackEvent> findByCandidateId(String candidateId);

    /**
     * v2.0 隐式信号聚合：返回某候选已关联的事件 + 同 chat 下尚未关联到任何候选的事件。
     * 后者覆盖"信号在候选创建前先发生"的场景（ConversationDistillJob 异步生成候选）。
     */
    @Query("SELECT e FROM WikiFeedbackEvent e " +
            "WHERE e.candidateId = :candidateId " +
            "   OR (e.candidateId IS NULL AND e.sourceChatId = :sourceChatId)")
    List<WikiFeedbackEvent> findForAggregation(
            @Param("candidateId") String candidateId,
            @Param("sourceChatId") String sourceChatId
    );
}
