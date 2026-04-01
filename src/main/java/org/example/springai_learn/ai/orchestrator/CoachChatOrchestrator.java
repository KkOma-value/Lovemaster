package org.example.springai_learn.ai.orchestrator;

import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.ChatMemory.DatabaseChatMemory;
import org.example.springai_learn.ai.context.BrainDecision;
import org.example.springai_learn.ai.context.ChatInputContext;
import org.example.springai_learn.ai.context.ConversationIds;
import org.example.springai_learn.ai.context.IntakeAnalysisResult;
import org.example.springai_learn.ai.context.ToolsAgentResult;
import org.example.springai_learn.ai.service.AiErrorMessageResolver;
import org.example.springai_learn.ai.service.BrainAgentService;
import org.example.springai_learn.ai.service.ChatRunService;
import org.example.springai_learn.ai.service.MultimodalIntakeService;
import org.example.springai_learn.ai.service.RagKnowledgeService;
import org.example.springai_learn.ai.service.SseEventHelper;
import org.example.springai_learn.ai.service.ToolsAgentService;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * Coach 模式编排器 — Brain→Tools→Brain 架构。
 * <p>
 * 流程：
 * 1. Intake 阶段：MultimodalIntakeService 处理文本/截图
 * 2. RAG 阶段：RagKnowledgeService 检索知识
 * 3. Brain 决策：BrainAgentService 判断是否需要工具
 * 4a. 不需要工具 → Brain 直接回答
 * 4b. 需要工具 → 激活 ToolsAgent 执行 → Brain 综合结果回答
 */
@Service
@Slf4j
public class CoachChatOrchestrator {

    private final MultimodalIntakeService multimodalIntakeService;
    private final RagKnowledgeService ragKnowledgeService;
    private final BrainAgentService brainAgentService;
    private final ToolsAgentService toolsAgentService;
    private final SseEventHelper sseEventHelper;
    private final DatabaseChatMemory databaseChatMemory;
    private final ChatRunService chatRunService;

    public CoachChatOrchestrator(
            MultimodalIntakeService multimodalIntakeService,
            RagKnowledgeService ragKnowledgeService,
            BrainAgentService brainAgentService,
            ToolsAgentService toolsAgentService,
            SseEventHelper sseEventHelper,
            DatabaseChatMemory databaseChatMemory,
            ChatRunService chatRunService) {
        this.multimodalIntakeService = multimodalIntakeService;
        this.ragKnowledgeService = ragKnowledgeService;
        this.brainAgentService = brainAgentService;
        this.toolsAgentService = toolsAgentService;
        this.sseEventHelper = sseEventHelper;
        this.databaseChatMemory = databaseChatMemory;
        this.chatRunService = chatRunService;
    }

    public SseEmitter stream(ChatInputContext context, String runId) {
        SseEmitter emitter = new SseEmitter(600_000L); // 10 分钟超时（工具执行可能较长）
        emitter.onTimeout(() -> {
            log.warn("CoachChatOrchestrator SSE 超时: chatId={}, runId={}", context.chatId(), runId);
            chatRunService.markFailedIfActive(runId, "请求超时");
        });
        sseEventHelper.send(emitter, "run_started", "", Map.of(
                "runId", runId,
                "chatId", context.chatId(),
                "chatType", "coach",
                "status", "QUEUED"
        ));

        // 立即持久化用户消息，避免刷新页面时丢失提问
        String conversationId = ConversationIds.forMode(context.userId(), context.mode(), context.chatId());
        databaseChatMemory.add(conversationId, List.of(new UserMessage(context.userMessage())));
        if (context.hasImage()) {
            databaseChatMemory.setImageUrlOnLatestUserMessage(context.chatId(), context.imageUrl());
        }

        Thread.startVirtualThread(() -> {
            try {
                // ── Step 1: Intake — OCR + 结构化重写 ──
                publishStatus(emitter, runId, "intake_status",
                        context.hasImage()
                                ? "宝，我正在看你的聊天截图，帮你整理一下..."
                                : "正在整理你的问题，帮你想清楚该怎么回...");
                IntakeAnalysisResult analysis = multimodalIntakeService.analyze(context);

                if (analysis.hasImage()) {
                    String ocrHint = analysis.ocrText() == null || analysis.ocrText().isBlank()
                            ? "截图识别完成，正在提炼关键上下文。"
                            : "截图识别完成：" + shorten(analysis.ocrText(), 88);
                    publishStatus(emitter, runId, "ocr_result", ocrHint);
                }
                publishStatus(emitter, runId, "rewrite_result",
                        "我已经把任务整理成更清晰的问题：" + shorten(analysis.rewrittenQuestion(), 96));

                // ── Step 2: RAG — 检索相关知识 ──
                publishStatus(emitter, runId, "rag_status", "正在查阅恋爱知识库，补充参考资料...");
                String ragKnowledge = ragKnowledgeService.retrieveKnowledge(analysis.rewrittenQuestion());

                // ── Step 3: Brain 决策 — 判断是否需要工具 ──
                publishStatus(emitter, runId, "status", "正在分析你的需求...");
                BrainDecision decision = brainAgentService.decide(context, analysis, ragKnowledge);
                publishStatus(emitter, runId, "status", decision.userFacingPrelude());

                if (!decision.needsTools()) {
                    // ── Path A: Brain 直接回答（无需工具） ──
                    log.info("BrainAgent 决定直接回答: chatId={}", context.chatId());
                    String answer = decision.directAnswer();
                    saveAssistantMessage(conversationId, answer);
                    chatRunService.appendContent(runId, answer);
                    sseEventHelper.send(emitter, "content", answer);
                    chatRunService.markCompleted(runId, answer);
                    sseEventHelper.done(emitter, terminalDonePayload(runId, context.chatId(), "coach"));
                    emitter.complete();
                    return;
                }

                // ── Path B: Brain 激活 ToolsAgent ──
                log.info("BrainAgent 激活 ToolsAgent: chatId={}", context.chatId());
                publishStatus(emitter, runId, "tool_call", "宝，我帮你查点资料整理一下，稍等哈~");

                // Step 4: ToolsAgent 执行工具任务（同步，file_created 事件通过 emitter 推送）
                ToolsAgentResult toolResult = toolsAgentService.activate(decision, conversationId, emitter);

                // Step 5: Brain 综合工具结果，生成最终回答（包含已存储图片 URL）
                publishStatus(emitter, runId, "status", "工具执行完成，正在综合结论...");
                String finalAnswer = brainAgentService.synthesize(
                        decision, toolResult.textResult(), toolResult.storedImages());

                // 保存 & 发送
                saveAssistantMessage(conversationId, finalAnswer);
                chatRunService.appendContent(runId, finalAnswer);
                sseEventHelper.send(emitter, "content", finalAnswer);
                chatRunService.markCompleted(runId, finalAnswer);
                sseEventHelper.done(emitter, terminalDonePayload(runId, context.chatId(), "coach"));
                emitter.complete();
            } catch (Exception e) {
                log.error("CoachChatOrchestrator 处理失败: {}", e.getMessage(), e);
                String resolved = "处理失败：" + AiErrorMessageResolver.resolve(e);
                chatRunService.markFailed(runId, resolved);
                sseEventHelper.send(emitter, "error", resolved);
                emitter.complete();
            }
        });

        return emitter;
    }

    /**
     * 仅保存 assistant 回复（用户消息已在 stream() 入口处提前持久化）。
     */
    private void saveAssistantMessage(String conversationId, String answer) {
        databaseChatMemory.add(conversationId, List.of(new AssistantMessage(answer)));
    }

    private String shorten(String text, int limit) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.length() <= limit ? text : text.substring(0, limit) + "...";
    }

    private void publishStatus(SseEmitter emitter, String runId, String type, String content) {
        chatRunService.recordEvent(runId, type, content);
        if ("intake_status".equals(type)) {
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
