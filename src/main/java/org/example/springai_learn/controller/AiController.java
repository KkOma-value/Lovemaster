package org.example.springai_learn.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.ai.context.ChatInputContext;
import org.example.springai_learn.ai.context.ChatMode;
import org.example.springai_learn.ai.context.ConversationIds;
import org.example.springai_learn.ai.orchestrator.CoachChatOrchestrator;
import org.example.springai_learn.ai.orchestrator.LoveChatOrchestrator;
import org.example.springai_learn.ai.service.ChatRunService;
import org.example.springai_learn.app.LoveApp;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/ai")
@CrossOrigin(originPatterns = "*", allowCredentials = "false")
@Slf4j
public class AiController {

    @Resource
    private LoveApp loveApp;

    @Resource
    private LoveChatOrchestrator loveChatOrchestrator;

    @Resource
    private CoachChatOrchestrator coachChatOrchestrator;

    @Resource
    private ChatRunService chatRunService;

    private String getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof String userId) {
            return userId;
        }
        return "anonymous";
    }

    @GetMapping("/love_app/chat/sync")
    public String doChatWithLoveAppSync(String message, String chatId) {
        log.info("同步聊天请求: message={}, chatId={}", message, chatId);
        String conversationId = ConversationIds.forType(getCurrentUserId(), "loveapp", chatId);
        return loveApp.doChat(message, conversationId);
    }

    @GetMapping(value = "/love_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter doChatWithLoveAppSSE(String message,
            String chatId,
            @RequestParam(required = false) String imageUrl) {
        log.info("开始 Love 模式 SSE: message={}, chatId={}, imageUrl={}", message, chatId, imageUrl);
        ChatInputContext context = new ChatInputContext(getCurrentUserId(), chatId, ChatMode.LOVE, message, imageUrl);
        String runId = chatRunService.createRun(getCurrentUserId(), "loveapp", chatId, message, imageUrl).getId();
        return loveChatOrchestrator.stream(context, runId);
    }

    /**
     * 流式调用 Manus 超级智能体
     *
     * @param message 用户消息
     * @param chatId  会话ID（用于持久化）
     * @return
     */
    @GetMapping(value = "/manus/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter doChatWithManus(String message,
            @RequestParam(required = false, defaultValue = "") String chatId,
            @RequestParam(required = false) String imageUrl) {
        log.info("开始 Coach 模式 SSE: message={}, chatId={}, imageUrl={}", message, chatId, imageUrl);
        ChatInputContext context = new ChatInputContext(getCurrentUserId(), chatId, ChatMode.COACH, message, imageUrl);
        String runId = chatRunService.createRun(getCurrentUserId(), "coach", chatId, message, imageUrl).getId();
        return coachChatOrchestrator.stream(context, runId);
    }
}
