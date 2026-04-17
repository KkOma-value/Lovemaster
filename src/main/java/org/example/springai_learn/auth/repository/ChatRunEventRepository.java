package org.example.springai_learn.auth.repository;

import org.example.springai_learn.auth.entity.ChatRunEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRunEventRepository extends JpaRepository<ChatRunEvent, String> {

    /**
     * 按 runId 查询所有事件，按时间正序排列。
     */
    List<ChatRunEvent> findByRunIdOrderByCreatedAtAsc(String runId);

    /**
     * 按 runId 和事件类型查询。
     */
    List<ChatRunEvent> findByRunIdAndEventTypeOrderByCreatedAtAsc(String runId, String eventType);
}
