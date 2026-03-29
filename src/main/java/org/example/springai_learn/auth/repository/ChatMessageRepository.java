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
}
