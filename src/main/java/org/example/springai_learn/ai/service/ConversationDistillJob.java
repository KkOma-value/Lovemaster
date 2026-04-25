package org.example.springai_learn.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.ai.dto.KnowledgeCandidateRequest;
import org.example.springai_learn.auth.entity.ChatMessage;
import org.example.springai_learn.auth.entity.Conversation;
import org.example.springai_learn.auth.repository.ChatMessageRepository;
import org.example.springai_learn.auth.repository.ConversationRepository;
import org.example.springai_learn.auth.repository.WikiCandidateRepository;
import org.example.springai_learn.config.KnowledgeProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * v2.0 隐式信号沉淀：扫描最近的 assistant 消息，自动生成 wiki 候选。
 *
 * 用户不再需要点击任何按钮（书签/收藏已废除）。本任务定期：
 *   1. 抓取最近 N 小时内长度 ≥ minContentLength 的 assistant 消息
 *   2. 用 sourceRunId = 消息 id 做幂等检查，避免重复落库
 *   3. 调用 KnowledgeSinkService 创建 pending_review 候选（triggerType=auto_distill）
 *   4. 后续由 SignalAggregator + KnowledgeAutoApprovalJob 决定是否自动批准
 *
 * 触发周期: 默认每 15 分钟。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConversationDistillJob {

    private static final String TRIGGER_TYPE = "auto_distill";

    private final KnowledgeProperties properties;
    private final ChatMessageRepository chatMessageRepository;
    private final ConversationRepository conversationRepository;
    private final WikiCandidateRepository wikiCandidateRepository;
    private final KnowledgeSinkService knowledgeSinkService;
    private final KnowledgeMetrics metrics;

    @Scheduled(cron = "${app.knowledge.auto-distill.cron:0 */15 * * * *}")
    public void distillRecentConversations() {
        KnowledgeProperties.AutoDistill config = properties.getAutoDistill();
        if (!config.isEnabled()) {
            return;
        }

        LocalDateTime since = LocalDateTime.now().minusHours(config.getLookbackHours());
        List<ChatMessage> recent = chatMessageRepository.findRecentAssistantMessages(
                since,
                config.getMinContentLength(),
                PageRequest.of(0, 200)
        );
        if (recent.isEmpty()) {
            log.debug("Auto-distill: no eligible assistant messages since {}", since);
            return;
        }

        int created = 0;
        int skipped = 0;
        int failed = 0;

        for (ChatMessage assistantMsg : recent) {
            if (wikiCandidateRepository.existsBySourceRunIdAndTriggerType(assistantMsg.getId(), TRIGGER_TYPE)) {
                skipped++;
                continue;
            }

            Conversation conversation = conversationRepository.findById(assistantMsg.getConversationId()).orElse(null);
            if (conversation == null) {
                skipped++;
                continue;
            }

            String userQuestion = findPriorUserContent(assistantMsg);
            try {
                knowledgeSinkService.createCandidate(conversation.getUserId(), new KnowledgeCandidateRequest(
                        conversation.getId(),
                        assistantMsg.getId(),
                        userQuestion,
                        assistantMsg.getContent(),
                        TRIGGER_TYPE,
                        null
                ));
                created++;
            } catch (Exception ex) {
                failed++;
                log.warn("Auto-distill failed for messageId={}, chatId={}: {}",
                        assistantMsg.getId(), conversation.getId(), ex.getMessage());
            }
        }

        metrics.autoDistillCreated(created);
        log.info("Auto-distill sweep done: scanned={}, created={}, skipped={}, failed={}",
                recent.size(), created, skipped, failed);
    }

    private String findPriorUserContent(ChatMessage assistantMsg) {
        List<ChatMessage> priors = chatMessageRepository.findPriorUserMessages(
                assistantMsg.getConversationId(),
                assistantMsg.getCreatedAt(),
                PageRequest.of(0, 1)
        );
        if (priors.isEmpty()) {
            return null;
        }
        return priors.get(0).getContent();
    }
}
