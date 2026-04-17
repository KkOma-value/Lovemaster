package org.example.springai_learn.ai.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.ChatMemory.DatabaseChatMemory;
import org.example.springai_learn.ai.context.ChatInputContext;
import org.example.springai_learn.ai.context.ConversationIds;
import org.example.springai_learn.ai.context.IntakeAnalysisResult;
import org.example.springai_learn.ai.context.ProbabilityAnalysis;
import org.example.springai_learn.ai.service.AiErrorMessageResolver;
import org.example.springai_learn.ai.service.ChatRunService;
import org.example.springai_learn.ai.service.MultimodalIntakeService;
import org.example.springai_learn.ai.service.ProbabilityAnalysisService;
import org.example.springai_learn.ai.service.RagKnowledgeService;
import org.example.springai_learn.ai.service.SseEventHelper;
import org.example.springai_learn.app.LoveApp;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoveChatOrchestrator {

    private final MultimodalIntakeService multimodalIntakeService;
    private final ProbabilityAnalysisService probabilityAnalysisService;
    private final RagKnowledgeService ragKnowledgeService;
    private final SseEventHelper sseEventHelper;
    private final LoveApp loveApp;
    private final DatabaseChatMemory databaseChatMemory;
    private final ChatRunService chatRunService;
    private final ObjectMapper objectMapper;

    public SseEmitter stream(ChatInputContext context, String runId) {
        SseEmitter emitter = new SseEmitter(300000L);
        sseEventHelper.registerLifecycle(emitter);
        emitter.onTimeout(() -> {
            log.warn("LoveChatOrchestrator SSE 超时: chatId={}, runId={}", context.chatId(), runId);
            chatRunService.markFailedIfActive(runId, "请求超时");
        });
        sseEventHelper.send(emitter, "run_started", "", Map.of(
                "runId", runId,
                "chatId", context.chatId(),
                "chatType", "loveapp",
                "status", "QUEUED"
        ));

        Thread.startVirtualThread(() -> {
            ProbabilityAnalysis probResult = null;
            try {
                // Step 1: Intake — OCR + structured rewrite
                if (context.hasImage()) {
                    publishStatus(emitter, runId, "intake_status", "正在识别聊天截图，整理双方对话...");
                } else {
                    publishStatus(emitter, runId, "thinking", "正在分析你的问题，准备回复建议...");
                }
                IntakeAnalysisResult analysis = multimodalIntakeService.analyze(context);

                if (analysis.hasImage()) {
                    String ocrHint = analysis.ocrText() == null || analysis.ocrText().isBlank()
                            ? "截图识别完成，正在提炼对话重点。"
                            : "截图识别完成：" + shorten(analysis.ocrText(), 88);
                    publishStatus(emitter, runId, "ocr_result", ocrHint);
                }

                publishStatus(emitter, runId, "rewrite_result",
                        "我已经把你的问题整理好了：" + shorten(analysis.rewrittenQuestion(), 96));

                // Step 2 (NEW): Probability analysis — if intent detected
                if (analysis.probabilityRequested()) {
                    publishStatus(emitter, runId, "probability_status", "正在评估成功概率...");
                    try {
                        probResult = probabilityAnalysisService.analyze(context, analysis);
                        if (probResult != null) {
                            sseEventHelper.send(emitter, "probability_result", "", Map.of(
                                    "runId", runId,
                                    "chatId", context.chatId(),
                                    "probability", probResult
                            ));
                            chatRunService.recordProbability(runId, probResult);
                        }
                    } catch (Exception e) {
                        log.warn("概率分析失败，跳过: chatId={}, error={}", context.chatId(), e.getMessage());
                        // 不抛出，继续走文字分析
                    }
                }

                // Step 3: RAG — retrieve relevant knowledge using rewritten question
                publishStatus(emitter, runId, "rag_status", "正在查阅恋爱知识库，补充参考资料...");
                String ragKnowledge = ragKnowledgeService.retrieveKnowledge(analysis.rewrittenQuestion());

                publishStatus(emitter, runId, "status", "正在生成对方意图分析和可直接发送的回复建议...");

                // Step 4: Build system context — inject intake analysis + RAG knowledge
                String systemContext = buildSystemContext(analysis, ragKnowledge, probResult);
                String conversationId = ConversationIds.forMode(context.userId(), context.mode(), context.chatId());

                // Capture probability for persistence in doOnComplete
                final ProbabilityAnalysis finalProbResult = probResult;

                // Step 5: Stream content chunks to client
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
                    // Persist probability analysis on the latest assistant message
                    if (finalProbResult != null) {
                        try {
                            String probJson = objectMapper.writeValueAsString(finalProbResult);
                            databaseChatMemory.setProbabilityOnLatestAssistantMessage(context.chatId(), probJson);
                        } catch (Exception e) {
                            log.warn("保存概率分析数据失败: {}", e.getMessage());
                        }
                    }
                    chatRunService.markCompleted(runId, null);
                    sseEventHelper.done(emitter, terminalDonePayload(runId, context.chatId(), "loveapp"));
                    emitter.complete();
                }).doOnError(e -> {
                    log.error("流式聊天失败: {}", e.getMessage(), e);
                    String resolved = "处理失败：" + AiErrorMessageResolver.resolve(e);
                    chatRunService.markFailed(runId, resolved);
                    sseEventHelper.send(emitter, "error", resolved);
                    emitter.complete();
                }).subscribe(chunk -> {
                    if (chunk != null && !chunk.isBlank()) {
                        chatRunService.appendContent(runId, chunk);
                        sseEventHelper.send(emitter, "content", chunk);
                    }
                });

            } catch (Exception e) {
                log.error("LoveChatOrchestrator 处理失败: {}", e.getMessage(), e);
                String resolved = "处理失败：" + AiErrorMessageResolver.resolve(e);
                chatRunService.markFailed(runId, resolved);
                sseEventHelper.send(emitter, "error", resolved);
                emitter.complete();
            }
        });

        return emitter;
    }

    private String buildSystemContext(IntakeAnalysisResult analysis, String ragKnowledge, ProbabilityAnalysis prob) {
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

        // If probability card was shown, instruct the model accordingly
        if (prob != null) {
            sb.append("\n# 概率分析已展示\n");
            sb.append("前置概率分析服务已给出成功概率 ").append(prob.probability()).append("% (").append(prob.tier()).append(")。\n");
            sb.append("请不要在回复中重复概率数字，只补充\"为什么\"和\"下一步怎么做\"。\n");
        }

        if (ragKnowledge != null && !ragKnowledge.isBlank()) {
            sb.append("\n# 相关知识参考\n").append(ragKnowledge).append("\n");
        }

        sb.append("""

                # 回答要求
                请像闺蜜一样亲切自然地回答，不要使用任何分段标签。
                先直接回应用户的核心问题，然后自然地展开分析。
                最后给2-3条可以直接发送的回复话术，用不同语气区分。
                短句为主，口语化表达。
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

    private void publishStatus(SseEmitter emitter, String runId, String type, String content) {
        chatRunService.recordEvent(runId, type, content);
        if ("thinking".equals(type)) {
            chatRunService.markRunning(runId, content);
        }
        sseEventHelper.send(emitter, type, content);
    }

    private Map<String, Object> terminalDonePayload(String runId, String chatId, String chatType) {
        return Map.of(
                "runId", runId,
                "chatId", chatId,
                "chatType", chatType,
                "status", "COMPLETED"
        );
    }
}
