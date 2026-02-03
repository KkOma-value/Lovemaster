package org.example.springai_learn.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.agent.KkomaManus;
import org.example.springai_learn.app.LoveApp;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
@CrossOrigin(originPatterns = "*", allowCredentials = "false")
@Slf4j
public class AiController {

    @Resource
    private LoveApp loveApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    @Autowired(required = false)
    @Qualifier("toolCallbacks")
    private ToolCallbackProvider mcpToolCallbackProvider;

    @GetMapping("/love_app/chat/sync")
    public String doChatWithLoveAppSync(String message, String chatId) {
        log.info("同步聊天请求: message={}, chatId={}", message, chatId);
        return loveApp.doChat(message, chatId);
    }

    @GetMapping(value = "/love_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> doChatWithLoveAppSSE(String message, String chatId) {
        log.info("开始SSE流式聊天: message={}, chatId={}", message, chatId);

        return loveApp.doChatByStream(message, chatId)
                .doOnNext(chunk -> log.info("SSE发送数据块: {}", chunk))
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build())
                .doOnComplete(() -> log.info("原始流完成: chatId={}", chatId))
                .concatWith(Flux.just(ServerSentEvent.<String>builder()
                        .data("[DONE]")
                        .build())
                        .doOnNext(done -> log.info("发送结束标记: [DONE], chatId={}", chatId)))
                .doOnComplete(() -> log.info("SSE流完成: chatId={}", chatId))
                .doOnError(error -> {
                    log.error("SSE流出错: chatId={}, error={}", chatId, error.getMessage());
                    // 不重新抛出IOException，避免日志污染
                    if (!(error instanceof java.io.IOException)) {
                        throw new RuntimeException(error);
                    }
                })
                .onErrorResume(java.io.IOException.class, e -> {
                    log.warn("客户端连接已关闭: chatId={}", chatId);
                    return Flux.empty(); // 连接关闭时返回空流
                });
    }

    /**
     * 流式调用 Manus 超级智能体
     *
     * @param message 用户消息
     * @param chatId  会话ID（用于持久化）
     * @return
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message,
            @RequestParam(required = false, defaultValue = "") String chatId) {
        log.info("开始Manus聊天: message={}, chatId={}", message, chatId);
        KkomaManus kkomaManus = new KkomaManus(allTools, dashscopeChatModel, mcpToolCallbackProvider, chatId);
        SseEmitter emitter = kkomaManus.runStream(message);

        // 在 SSE 完成后保存对话到 ChatMemory
        emitter.onCompletion(() -> {
            log.info("SSE完成，保存对话到ChatMemory: chatId={}", chatId);
            kkomaManus.saveToChatMemory();
        });
        emitter.onError(ex -> {
            log.error("SSE错误，尝试保存对话: chatId={}, error={}", chatId, ex.getMessage());
            kkomaManus.saveToChatMemory();
        });

        return emitter;
    }
}
