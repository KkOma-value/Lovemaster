package org.example.springai_learn.auth.repository;

import org.example.springai_learn.auth.entity.ChatRun;
import org.example.springai_learn.auth.entity.ChatRunStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChatRunRepository extends JpaRepository<ChatRun, String> {

    List<ChatRun> findByUserIdAndStatusInOrderByCreatedAtDesc(String userId, Collection<ChatRunStatus> statuses);

    Optional<ChatRun> findByIdAndUserId(String id, String userId);

    Optional<ChatRun> findFirstByUserIdAndChatIdAndChatTypeOrderByCreatedAtDesc(
            String userId,
            String chatId,
            String chatType
    );
}
