package org.example.springai_learn.auth.repository;

import org.example.springai_learn.auth.entity.ConversationImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationImageRepository extends JpaRepository<ConversationImage, String> {
    List<ConversationImage> findByConversationIdOrderByCreatedAtAsc(String conversationId);
    void deleteByConversationId(String conversationId);
    Optional<ConversationImage> findFirstByStoragePath(String storagePath);
    Optional<ConversationImage> findFirstByConversationIdAndSourceUrl(String conversationId, String sourceUrl);
}
