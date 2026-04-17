package org.example.springai_learn.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.ChatMemory.DatabaseChatMemory;
import org.example.springai_learn.auth.entity.ChatMessage;
import org.example.springai_learn.auth.entity.Conversation;
import org.example.springai_learn.auth.entity.ConversationImage;
import org.example.springai_learn.auth.repository.ConversationImageRepository;
import org.example.springai_learn.auth.repository.ConversationRepository;
import org.example.springai_learn.dto.ChatSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * 会话管理控制器
 * 基于数据库存储（Supabase PostgreSQL）
 */
@RestController
@RequestMapping("/ai/sessions")
@CrossOrigin(originPatterns = "*", allowCredentials = "false")
@RequiredArgsConstructor
@Slf4j
public class ChatSessionController {

    private final DatabaseChatMemory databaseChatMemory;
    private final ConversationImageRepository conversationImageRepository;
    private final ConversationRepository conversationRepository;
    private final ObjectMapper objectMapper;

    private String getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof String userId) {
            return userId;
        }
        return "anonymous";
    }

    /**
     * 获取所有会话列表
     */
    @GetMapping
    public List<ChatSession> listSessions(
            @RequestParam(defaultValue = "loveapp") String chatType) {
        String userId = getCurrentUserId();
        log.info("获取会话列表: chatType={}, userId={}", chatType, userId);

        List<Conversation> conversations = databaseChatMemory.listConversations(userId, chatType);
        return conversations.stream()
                .map(conv -> new ChatSession(conv.getId(), conv.getTitle()))
                .toList();
    }

    /**
     * 删除指定会话
     */
    @DeleteMapping("/{chatId}")
    public ResponseEntity<Map<String, Object>> deleteSession(
            @PathVariable String chatId,
            @RequestParam(defaultValue = "loveapp") String chatType) {
        String userId = getCurrentUserId();
        log.info("删除会话: chatId={}, chatType={}, userId={}", chatId, chatType, userId);
        try {
            databaseChatMemory.clear(chatId);
            return ResponseEntity.ok(Map.of("success", true, "message", "会话已删除"));
        } catch (Exception e) {
            log.error("删除会话失败: {}", chatId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "删除失败: " + e.getMessage()));
        }
    }

    /**
     * 获取指定会话的消息历史
     * assistant 消息会携带 probability 字段（JSON 对象，若有）
     */
    @GetMapping("/{chatId}/messages")
    public List<Map<String, Object>> getMessages(
            @PathVariable String chatId,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "loveapp") String chatType) {
        String userId = getCurrentUserId();
        log.info("获取会话消息: chatId={}, limit={}, chatType={}, userId={}", chatId, limit, chatType, userId);

        List<ChatMessage> messages = databaseChatMemory.getAllMessages(chatId);
        return messages.stream()
                .map(msg -> {
                    Map<String, Object> entry = new java.util.LinkedHashMap<>();
                    entry.put("role", msg.getRole());
                    entry.put("content", msg.getContent());
                    if (msg.getImageUrl() != null && !msg.getImageUrl().isBlank()) {
                        entry.put("imageUrl", msg.getImageUrl());
                    }
                    // Return probability as parsed JSON object (not string) for frontend consumption
                    if (msg.getProbabilityJson() != null && !msg.getProbabilityJson().isBlank()) {
                        try {
                            Object probObj = objectMapper.readValue(msg.getProbabilityJson(), Object.class);
                            entry.put("probability", probObj);
                        } catch (Exception e) {
                            log.warn("解析 probabilityJson 失败: msgId={}, error={}", msg.getId(), e.getMessage());
                        }
                    }
                    return entry;
                })
                .toList();
    }

    /**
     * 获取指定会话的图片列表
     */
    @GetMapping("/{chatId}/images")
    public List<Map<String, String>> getImages(
            @PathVariable String chatId,
            @RequestParam(defaultValue = "coach") String chatType) {
        String userId = getCurrentUserId();
        log.info("获取会话图片: chatId={}, chatType={}, userId={}", chatId, chatType, userId);

        conversationRepository.findByIdAndUserIdAndChatType(chatId, userId, chatType)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "会话不存在"));

        List<ConversationImage> images = conversationImageRepository
                .findByConversationIdOrderByCreatedAtAsc(chatId);
        return images.stream()
                .map(img -> Map.of(
                        "type", img.getFileType() != null ? img.getFileType() : "image",
                        "name", img.getFileName() != null ? img.getFileName() : "",
                        "url", img.getPublicUrl() != null ? img.getPublicUrl() : ""))
                .toList();
    }
}
