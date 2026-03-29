package org.example.springai_learn.ai.orchestrator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.ChatMemory.DatabaseChatMemory;
import org.example.springai_learn.ai.context.ChatInputContext;
import org.example.springai_learn.ai.context.ConversationIds;
import org.example.springai_learn.ai.context.IntakeAnalysisResult;
import org.example.springai_learn.ai.service.AiErrorMessageResolver;
import org.example.springai_learn.ai.service.MultimodalIntakeService;
import org.example.springai_learn.ai.service.RagKnowledgeService;
import org.example.springai_learn.ai.service.SseEventHelper;
import org.example.springai_learn.app.LoveApp;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoveChatOrchestrator {

    private final MultimodalIntakeService multimodalIntakeService;
    private final RagKnowledgeService ragKnowledgeService;
    private final SseEventHelper sseEventHelper;
    private final LoveApp loveApp;
    private final DatabaseChatMemory databaseChatMemory;

    public SseEmitter stream(ChatInputContext context) {
        SseEmitter emitter = new SseEmitter(300000L);
        emitter.onTimeout(() -> log.warn("LoveChatOrchestrator SSE 超时: chatId={}", context.chatId()));

        Thread.startVirtualThread(() -> {
            try {
                // Step 1: Intake — OCR + structured rewrite
                if (context.hasImage()) {
                    sseEventHelper.send(emitter, "intake_status", "正在识别聊天截图，整理双方对话...");
                } else {
                    sseEventHelper.send(emitter, "thinking", "正在分析你的问题，准备回复建议...");
                }
                IntakeAnalysisResult analysis = multimodalIntakeService.analyze(context);

                if (analysis.hasImage()) {
                    String ocrHint = analysis.ocrText() == null || analysis.ocrText().isBlank()
                            ? "截图识别完成，正在提炼对话重点。"
                            : "截图识别完成：" + shorten(analysis.ocrText(), 88);
                    sseEventHelper.send(emitter, "ocr_result", ocrHint);
                }

                sseEventHelper.send(emitter, "rewrite_result",
                        "我已经把你的问题整理好了：" + shorten(analysis.rewrittenQuestion(), 96));

                // Step 2: RAG — retrieve relevant knowledge using rewritten question
                sseEventHelper.send(emitter, "rag_status", "正在查阅恋爱知识库，补充参考资料...");
                String ragKnowledge = ragKnowledgeService.retrieveKnowledge(analysis.rewrittenQuestion());

                sseEventHelper.send(emitter, "status", "正在生成对方意图分析和可直接发送的回复建议...");

                // Step 3: Build system context — inject intake analysis + RAG knowledge
                String systemContext = buildSystemContext(analysis, ragKnowledge);
                String conversationId = ConversationIds.forMode(context.userId(), context.mode(), context.chatId());

                // Step 4: Stream content chunks to client
                loveApp.doChatWithRAGContextStream(
                        safe(context.userMessage()), systemContext, conversationId
                ).doOnComplete(() -> {
                    if (context.hasImage()) {
                        try {
                            databaseChatMemory.setImageUrlOnLatestUserMessage(context.chatId(), context.imageUrl());
                        } catch (Exception e) {
                            log.warn("保存 imageUrl 失败: {}", e.getMessage());
                        }
                    }
                    sseEventHelper.done(emitter);
                    emitter.complete();
                }).doOnError(e -> {
                    log.error("流式聊天失败: {}", e.getMessage(), e);
                    sseEventHelper.send(emitter, "error", "处理失败：" + AiErrorMessageResolver.resolve(e));
                    emitter.complete();
                }).subscribe(chunk -> {
                    if (chunk != null && !chunk.isBlank()) {
                        sseEventHelper.send(emitter, "content", chunk);
                    }
                });

            } catch (Exception e) {
                log.error("LoveChatOrchestrator 处理失败: {}", e.getMessage(), e);
                sseEventHelper.send(emitter, "error", "处理失败：" + AiErrorMessageResolver.resolve(e));
                emitter.complete();
            }
        });

        return emitter;
    }

    private String buildSystemContext(IntakeAnalysisResult analysis, String ragKnowledge) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 当前请求分析\n");
        sb.append("对话摘要：").append(safe(analysis.conversationSummary())).append("\n");
        if (analysis.hasImage() && analysis.ocrText() != null && !analysis.ocrText().isBlank()) {
            sb.append("截图摘录:").append(shorten(analysis.ocrText(), 200)).append("\n");
        }
        sb.append("重写后的问题:").append(safe(analysis.rewrittenQuestion())).append("\n");
        if (!analysis.uncertainties().isEmpty()) {
            sb.append("不确定项:").append(String.join("；", analysis.uncertainties())).append("\n");
        }
        sb.append("用户意图:").append(safe(analysis.suggestedIntent())).append("\n");

        if (ragKnowledge != null && !ragKnowledge.isBlank()) {
            sb.append("\n# 相关知识参考\n").append(ragKnowledge).append("\n");
        }

        sb.append("""

                # 回答要求
                请按以下结构回答:
                1. 先解释对方现在更可能是什么意思
                2. 再说用户更好的策略
                3. 最后给 2-3 条可直接发送的话术，明确区分语气
                """);
        return sb.toString();
    }

    private String shorten(String text, int limit) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.length() <= limit ? text : text.substring(0, limit) + "...";
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }
}
