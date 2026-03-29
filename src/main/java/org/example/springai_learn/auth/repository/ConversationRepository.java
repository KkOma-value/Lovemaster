package org.example.springai_learn.auth.repository;

import org.example.springai_learn.auth.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, String> {
    List<Conversation> findByUserIdAndChatTypeOrderByUpdatedAtDesc(String userId, String chatType);
    List<Conversation> findByUserIdOrderByUpdatedAtDesc(String userId);
}
