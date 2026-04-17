package org.example.springai_learn.ChatMemory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.auth.entity.ChatMessage;
import org.example.springai_learn.auth.entity.Conversation;
import org.example.springai_learn.auth.repository.ChatMessageRepository;
import org.example.springai_learn.auth.repository.ConversationImageRepository;
import org.example.springai_learn.auth.repository.ConversationRepository;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 基于数据库（Supabase PostgreSQL）的对话记忆实现，
 * 替换原有的 FileBasedChatMemory。
 *
 * conversationId 格式约定："{userId}:{chatType}:{chatId}"
 * 其中 chatType 为 loveapp 或 coach。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseChatMemory implements ChatMemory {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ConversationImageRepository conversationImageRepository;

    @Override
    @Transactional
    public void add(String conversationId, List<Message> messages) {
        // 确保 conversation 存在
        ensureConversation(conversationId);

        for (Message message : messages) {
            ChatMessage entity = ChatMessage.builder()
                    .conversationId(extractChatId(conversationId))
                    .role(message.getMessageType().name().toLowerCase())
                    .content(message.getText())
                    .build();
            chatMessageRepository.save(entity);
        }

        // 更新会话标题（用第一条用户消息）和 updatedAt
        String chatId = extractChatId(conversationId);
        conversationRepository.findById(chatId).ifPresent(conv -> {
            if ("新的对话".equals(conv.getTitle())) {
                messages.stream()
                        .filter(m -> m.getMessageType() == MessageType.USER)
                        .findFirst()
                        .ifPresent(m -> {
                            String text = m.getText();
                            conv.setTitle(text.length() > 50 ? text.substring(0, 50) + "..." : text);
                        });
            }
            conv.setUpdatedAt(java.time.LocalDateTime.now());
            conversationRepository.save(conv);
        });
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        String chatId = extractChatId(conversationId);
        List<ChatMessage> entities = chatMessageRepository
                .findLatestMessages(chatId, PageRequest.of(0, lastN));

        // findLatestMessages 是 DESC 排序，需要反转为时间正序
        List<ChatMessage> reversed = new ArrayList<>(entities);
        Collections.reverse(reversed);

        return reversed.stream()
                .map(this::toSpringMessage)
                .toList();
    }

    @Override
    @Transactional
    public void clear(String conversationId) {
        String chatId = extractChatId(conversationId);
        chatMessageRepository.deleteByConversationId(chatId);
        conversationImageRepository.deleteByConversationId(chatId);
        conversationRepository.deleteById(chatId);
        log.info("已清除会话: {}", chatId);
    }

    /**
     * 替换会话的消息历史，但保留会话本身及其关联资源（如已下载图片）。
     */
    @Transactional
    public void replaceMessages(String conversationId, List<Message> messages) {
        String chatId = extractChatId(conversationId);
        ensureConversation(conversationId);
        chatMessageRepository.deleteByConversationId(chatId);
        add(conversationId, messages);
        log.info("已替换会话消息: {}", chatId);
    }

    /**
     * 获取指定用户和类型的所有会话列表
     */
    public List<Conversation> listConversations(String userId, String chatType) {
        return conversationRepository.findByUserIdAndChatTypeOrderByUpdatedAtDesc(userId, chatType);
    }

    /**
     * 获取会话的全部消息（按时间正序）
     */
    public List<ChatMessage> getAllMessages(String conversationId) {
        String chatId = extractChatId(conversationId);
        return chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(chatId);
    }

    /**
     * 更新指定会话中最近一条用户消息的 imageUrl 字段。
     * 用于在消息已由 MessageChatMemoryAdvisor 保存后补充图片引用。
     *
     * @param chatId   会话 ID（纯 chatId，非 composite key）
     * @param imageUrl 图片 URL
     */
    @Transactional
    public void setImageUrlOnLatestUserMessage(String chatId, String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        List<ChatMessage> recent = chatMessageRepository.findLatestMessages(chatId, PageRequest.of(0, 10));
        recent.stream()
                .filter(m -> "user".equals(m.getRole()))
                .findFirst()
                .ifPresentOrElse(m -> {
                    m.setImageUrl(imageUrl);
                    chatMessageRepository.save(m);
                    log.info("已更新用户消息 imageUrl: msgId={}, chatId={}", m.getId(), chatId);
                }, () -> log.warn("未找到可更新的用户消息: chatId={}", chatId));
    }

    /**
     * 更新指定会话中最近一条 assistant 消息的 probabilityJson 字段。
     * 用于在消息已由 MessageChatMemoryAdvisor 保存后补充概率分析结果。
     *
     * @param chatId          会话 ID（纯 chatId，非 composite key）
     * @param probabilityJson 概率分析的 JSON 字符串
     */
    @Transactional
    public void setProbabilityOnLatestAssistantMessage(String chatId, String probabilityJson) {
        if (probabilityJson == null || probabilityJson.isBlank()) {
            return;
        }
        List<ChatMessage> recent = chatMessageRepository.findLatestMessages(chatId, PageRequest.of(0, 10));
        recent.stream()
                .filter(m -> "assistant".equals(m.getRole()))
                .findFirst()
                .ifPresentOrElse(m -> {
                    m.setProbabilityJson(probabilityJson);
                    chatMessageRepository.save(m);
                    log.info("已更新 assistant 消息 probabilityJson: msgId={}, chatId={}", m.getId(), chatId);
                }, () -> log.warn("未找到可更新的 assistant 消息: chatId={}", chatId));
    }

    /**
     * 创建一个新会话，返回会话 ID
     */
    @Transactional
    public String createConversation(String userId, String chatType) {
        Conversation conv = Conversation.builder()
                .userId(userId)
                .chatType(chatType)
                .build();
        conversationRepository.save(conv);
        log.info("创建会话: id={}, userId={}, chatType={}", conv.getId(), userId, chatType);
        return conv.getId();
    }

    /**
     * 确保 conversation 记录存在。
     * 如果 conversationId 包含用户信息（userId:chatType:chatId），自动创建。
     */
    @Transactional
    public void ensureConversationExists(String conversationId) {
        ensureConversation(conversationId);
    }

    private void ensureConversation(String conversationId) {
        String chatId = extractChatId(conversationId);
        if (!conversationRepository.existsById(chatId)) {
            String userId = extractUserId(conversationId);
            String chatType = extractChatType(conversationId);
            Conversation conv = Conversation.builder()
                    .id(chatId)
                    .userId(userId)
                    .chatType(chatType)
                    .build();
            conversationRepository.save(conv);
            log.info("自动创建会话: id={}, userId={}, chatType={}", chatId, userId, chatType);
        }
    }

    private Message toSpringMessage(ChatMessage entity) {
        return switch (entity.getRole()) {
            case "user" -> new UserMessage(entity.getContent());
            case "assistant" -> new AssistantMessage(entity.getContent());
            case "system" -> new SystemMessage(entity.getContent());
            default -> new UserMessage(entity.getContent());
        };
    }

    // ---- conversationId 解析工具方法 ----
    // 格式: "userId:chatType:chatId" 或纯 "chatId"

    private String extractChatId(String conversationId) {
        String[] parts = conversationId.split(":");
        return parts.length >= 3 ? parts[2] : conversationId;
    }

    private String extractUserId(String conversationId) {
        String[] parts = conversationId.split(":");
        return parts.length >= 1 ? parts[0] : "anonymous";
    }

    private String extractChatType(String conversationId) {
        String[] parts = conversationId.split(":");
        return parts.length >= 2 ? parts[1] : "loveapp";
    }
}
