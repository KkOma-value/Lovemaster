package org.example.springai_learn.ai.orchestrator;

import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.ChatMemory.DatabaseChatMemory;
import org.example.springai_learn.ai.context.BrainDecision;
import org.example.springai_learn.ai.context.ChatInputContext;
import org.example.springai_learn.ai.context.ConversationIds;
import org.example.springai_learn.ai.context.IntakeAnalysisResult;
import org.example.springai_learn.ai.service.AiErrorMessageResolver;
import org.example.springai_learn.ai.service.BrainAgentService;
import org.example.springai_learn.ai.service.MultimodalIntakeService;
import org.example.springai_learn.ai.service.RagKnowledgeService;
import org.example.springai_learn.ai.service.SseEventHelper;
import org.example.springai_learn.ai.service.ToolsAgentService;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

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

    public CoachChatOrchestrator(
            MultimodalIntakeService multimodalIntakeService,
            RagKnowledgeService ragKnowledgeService,
            BrainAgentService brainAgentService,
            ToolsAgentService toolsAgentService,
            SseEventHelper sseEventHelper,
            DatabaseChatMemory databaseChatMemory) {
        this.multimodalIntakeService = multimodalIntakeService;
        this.ragKnowledgeService = ragKnowledgeService;
        this.brainAgentService = brainAgentService;
        this.toolsAgentService = toolsAgentService;
        this.sseEventHelper = sseEventHelper;
        this.databaseChatMemory = databaseChatMemory;
    }

    public SseEmitter stream(ChatInputContext context) {
        SseEmitter emitter = new SseEmitter(600_000L); // 10 分钟超时（工具执行可能较长）
        emitter.onTimeout(() -> log.warn("CoachChatOrchestrator SSE 超时: chatId={}", context.chatId()));

        Thread.startVirtualThread(() -> {
            try {
                // ── Step 1: Intake — OCR + 结构化重写 ──
                sseEventHelper.send(emitter, "intake_status",
                        context.hasImage()
                                ? "宝，我正在看你的聊天截图，帮你整理一下..."
                                : "正在整理你的问题，帮你想清楚该怎么回...");
                IntakeAnalysisResult analysis = multimodalIntakeService.analyze(context);

                if (analysis.hasImage()) {
                    String ocrHint = analysis.ocrText() == null || analysis.ocrText().isBlank()
                            ? "截图识别完成，正在提炼关键上下文。"
                            : "截图识别完成：" + shorten(analysis.ocrText(), 88);
                    sseEventHelper.send(emitter, "ocr_result", ocrHint);
                }
                sseEventHelper.send(emitter, "rewrite_result",
                        "我已经把任务整理成更清晰的问题：" + shorten(analysis.rewrittenQuestion(), 96));

                // ── Step 2: RAG — 检索相关知识 ──
                sseEventHelper.send(emitter, "rag_status", "正在查阅恋爱知识库，补充参考资料...");
                String ragKnowledge = ragKnowledgeService.retrieveKnowledge(analysis.rewrittenQuestion());

                // ── Step 3: Brain 决策 — 判断是否需要工具 ──
                sseEventHelper.send(emitter, "status", "正在分析你的需求...");
                BrainDecision decision = brainAgentService.decide(context, analysis, ragKnowledge);
                sseEventHelper.send(emitter, "status", decision.userFacingPrelude());

                String conversationId = ConversationIds.forMode(context.userId(), context.mode(), context.chatId());

                if (!decision.needsTools()) {
                    // ── Path A: Brain 直接回答（无需工具） ──
                    log.info("BrainAgent 决定直接回答: chatId={}", context.chatId());
                    String answer = decision.directAnswer();
                    saveConversation(conversationId, context, answer);
                    sseEventHelper.send(emitter, "content", answer);
                    sseEventHelper.done(emitter);
                    emitter.complete();
                    return;
                }

                // ── Path B: Brain 激活 ToolsAgent ──
                log.info("BrainAgent 激活 ToolsAgent: chatId={}", context.chatId());
                sseEventHelper.send(emitter, "tool_call", "宝，我帮你查点资料整理一下，稍等哈~");

                // Step 4: ToolsAgent 执行工具任务（同步，file_created 事件通过 emitter 推送）
                String toolResult = toolsAgentService.activate(decision, conversationId, emitter);

                // Step 5: Brain 综合工具结果，生成最终回答
                sseEventHelper.send(emitter, "status", "工具执行完成，正在综合结论...");
                String finalAnswer = brainAgentService.synthesize(decision, toolResult);

                // 保存 & 发送
                saveConversation(conversationId, context, finalAnswer);
                sseEventHelper.send(emitter, "content", finalAnswer);
                sseEventHelper.done(emitter);
                emitter.complete();
            } catch (Exception e) {
                log.error("CoachChatOrchestrator 处理失败: {}", e.getMessage(), e);
                sseEventHelper.send(emitter, "error", "处理失败：" + AiErrorMessageResolver.resolve(e));
                emitter.complete();
            }
        });

        return emitter;
    }

    private void saveConversation(String conversationId, ChatInputContext context, String answer) {
        databaseChatMemory.add(conversationId, List.of(
                new UserMessage(context.userMessage()),
                new AssistantMessage(answer)
        ));
        if (context.hasImage()) {
            databaseChatMemory.setImageUrlOnLatestUserMessage(context.chatId(), context.imageUrl());
        }
    }

    private String shorten(String text, int limit) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.length() <= limit ? text : text.substring(0, limit) + "...";
    }
}
