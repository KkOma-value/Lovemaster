package org.example.springai_learn.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.ChatMemory.FileBasedChatMemory;
import org.example.springai_learn.dto.ChatSession;
import org.springframework.ai.chat.messages.Message;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话管理控制器
 * 支持不同聊天类型使用不同的存储目录
 */
@RestController
@RequestMapping("/ai/sessions")
@CrossOrigin(originPatterns = "*", allowCredentials = "false")
@Slf4j
public class ChatSessionController {

    private static final String BASE_DIR = System.getProperty("user.dir") + "/tmp";

    // 缓存不同类型的 ChatMemory 实例
    private final Map<String, FileBasedChatMemory> memoryCache = new ConcurrentHashMap<>();

    /**
     * 根据聊天类型获取对应的 ChatMemory
     * 
     * @param chatType loveapp 或 coach
     */
    private FileBasedChatMemory getMemoryForType(String chatType) {
        String type = chatType != null ? chatType.toLowerCase() : "loveapp";
        return memoryCache.computeIfAbsent(type, t -> {
            String dir = switch (t) {
                case "coach" -> BASE_DIR + "/chat-coach";
                default -> BASE_DIR + "/chat-memory"; // loveapp 或默认
            };
            log.info("创建 ChatMemory 实例: type={}, dir={}", t, dir);
            return new FileBasedChatMemory(dir);
        });
    }

    /**
     * 获取所有会话列表
     * 
     * @param chatType 聊天类型: loveapp 或 coach
     */
    @GetMapping
    public List<ChatSession> listSessions(
            @RequestParam(defaultValue = "loveapp") String chatType) {
        log.info("获取会话列表: chatType={}", chatType);
        FileBasedChatMemory chatMemory = getMemoryForType(chatType);
        List<String> conversationIds = chatMemory.listConversationIds();

        return conversationIds.stream()
                .map(id -> {
                    // 尝试获取会话的第一条用户消息作为标题
                    List<Message> messages = chatMemory.get(id, 2);
                    String title = "新的对话";
                    for (Message msg : messages) {
                        if ("USER".equals(msg.getMessageType().name())) {
                            String content = msg.getText();
                            title = content.length() > 20
                                    ? content.substring(0, 20) + "..."
                                    : content;
                            break;
                        }
                    }
                    return new ChatSession(id, title);
                })
                .toList();
    }

    /**
     * 删除指定会话
     * 
     * @param chatType 聊天类型: loveapp 或 coach
     */
    @DeleteMapping("/{chatId}")
    public ResponseEntity<Map<String, Object>> deleteSession(
            @PathVariable String chatId,
            @RequestParam(defaultValue = "loveapp") String chatType) {
        log.info("删除会话: chatId={}, chatType={}", chatId, chatType);
        try {
            FileBasedChatMemory chatMemory = getMemoryForType(chatType);
            chatMemory.clear(chatId);
            return ResponseEntity.ok(Map.of("success", true, "message", "会话已删除"));
        } catch (Exception e) {
            log.error("删除会话失败: {}", chatId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "删除失败: " + e.getMessage()));
        }
    }

    /**
     * 获取指定会话的消息历史
     * 
     * @param chatType 聊天类型: loveapp 或 coach
     */
    @GetMapping("/{chatId}/messages")
    public List<Map<String, String>> getMessages(
            @PathVariable String chatId,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "loveapp") String chatType) {
        log.info("获取会话消息: chatId={}, limit={}, chatType={}", chatId, limit, chatType);
        FileBasedChatMemory chatMemory = getMemoryForType(chatType);
        List<Message> messages = chatMemory.get(chatId, limit);

        return messages.stream()
                .map(msg -> Map.of(
                        "role", msg.getMessageType().name().toLowerCase(),
                        "content", msg.getText()))
                .toList();
    }
}
