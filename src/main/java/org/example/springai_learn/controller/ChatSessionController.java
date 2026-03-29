package org.example.springai_learn.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.ChatMemory.DatabaseChatMemory;
import org.example.springai_learn.auth.entity.ChatMessage;
import org.example.springai_learn.auth.entity.Conversation;
import org.example.springai_learn.dto.ChatSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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
     */
    @GetMapping("/{chatId}/messages")
    public List<Map<String, String>> getMessages(
            @PathVariable String chatId,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "loveapp") String chatType) {
        String userId = getCurrentUserId();
        log.info("获取会话消息: chatId={}, limit={}, chatType={}, userId={}", chatId, limit, chatType, userId);

        List<ChatMessage> messages = databaseChatMemory.getAllMessages(chatId);
        return messages.stream()
                .map(msg -> Map.of(
                        "role", msg.getRole(),
                        "content", msg.getContent()))
                .toList();
    }
}
