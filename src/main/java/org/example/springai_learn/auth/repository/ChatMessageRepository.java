package org.example.springai_learn.auth.repository;

import org.example.springai_learn.auth.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    @Query("SELECT m FROM ChatMessage m WHERE m.conversationId = :conversationId ORDER BY m.createdAt DESC")
    List<ChatMessage> findLatestMessages(String conversationId, Pageable pageable);

    void deleteByConversationId(String conversationId);

    long countByConversationId(String conversationId);

    /**
     * v2.0 ConversationDistillJob 扫描：最近 N 小时内 role=assistant 且内容达标的消息。
     */
    @Query("SELECT m FROM ChatMessage m " +
            "WHERE m.role = 'assistant' " +
            "  AND m.createdAt >= :since " +
            "  AND LENGTH(m.content) >= :minLength " +
            "ORDER BY m.createdAt DESC")
    List<ChatMessage> findRecentAssistantMessages(
            @org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since,
            @org.springframework.data.repository.query.Param("minLength") int minLength,
            Pageable pageable
    );

    /**
     * 找到指定会话中某条消息之前的最后一条 user 消息（用于重建 Q-A 对）。
     */
    @Query("SELECT m FROM ChatMessage m " +
            "WHERE m.conversationId = :conversationId " +
            "  AND m.role = 'user' " +
            "  AND m.createdAt < :before " +
            "ORDER BY m.createdAt DESC")
    List<ChatMessage> findPriorUserMessages(
            @org.springframework.data.repository.query.Param("conversationId") String conversationId,
            @org.springframework.data.repository.query.Param("before") java.time.LocalDateTime before,
            Pageable pageable
    );
}
