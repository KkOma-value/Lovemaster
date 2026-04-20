package org.example.springai_learn.ai.orchestrator;

import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.ChatMemory.DatabaseChatMemory;
import org.example.springai_learn.ai.config.RewriteProperties;
import org.example.springai_learn.ai.context.BrainDecision;
import org.example.springai_learn.ai.context.ChatInputContext;
import org.example.springai_learn.ai.context.ConversationIds;
import org.example.springai_learn.ai.context.IntakeAnalysisResult;
import org.example.springai_learn.ai.context.OcrExtractionResult;
import org.example.springai_learn.ai.context.ToolsAgentResult;
import org.example.springai_learn.ai.service.AiErrorMessageResolver;
import org.example.springai_learn.ai.service.BrainAgentService;
import org.example.springai_learn.ai.service.ChatRunService;
import org.example.springai_learn.ai.service.MultimodalIntakeService;
import org.example.springai_learn.ai.service.OcrAgentService;
import org.example.springai_learn.ai.service.ProbabilityKeywordDetector;
import org.example.springai_learn.ai.service.RagKnowledgeService;
import org.example.springai_learn.ai.service.SseEventHelper;
import org.example.springai_learn.ai.service.ToolHintDetector;
import org.example.springai_learn.ai.service.ToolsAgentService;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Coach 模式编排器 — Brain→Tools→Brain 架构。
 * <p>
 * 新链路：
 * 1. 有图 → OcrAgentService 抽取（不改写）；无图 → 跳过
 * 2. RAG 查询串 = ocrSummary + userMessage（有图）或 userMessage（无图）
 * 3. Brain 决策 → 工具 or 直答
 * legacy-mode=true 时回退旧 MultimodalIntakeService。
 */
@Service
@Slf4j
public class CoachChatOrchestrator {

    private final MultimodalIntakeService multimodalIntakeService;
    private final OcrAgentService ocrAgentService;
    private final ProbabilityKeywordDetector probabilityKeywordDetector;
    private final ToolHintDetector toolHintDetector;
    private final RewriteProperties rewriteProperties;
    private final RagKnowledgeService ragKnowledgeService;
    private final BrainAgentService brainAgentService;
    private final ToolsAgentService toolsAgentService;
    private final SseEventHelper sseEventHelper;
    private final DatabaseChatMemory databaseChatMemory;
    private final ChatRunService chatRunService;

    public CoachChatOrchestrator(
            MultimodalIntakeService multimodalIntakeService,
            OcrAgentService ocrAgentService,
            ProbabilityKeywordDetector probabilityKeywordDetector,
            ToolHintDetector toolHintDetector,
            RewriteProperties rewriteProperties,
            RagKnowledgeService ragKnowledgeService,
            BrainAgentService brainAgentService,
            ToolsAgentService toolsAgentService,
            SseEventHelper sseEventHelper,
            DatabaseChatMemory databaseChatMemory,
            ChatRunService chatRunService) {
        this.multimodalIntakeService = multimodalIntakeService;
        this.ocrAgentService = ocrAgentService;
        this.probabilityKeywordDetector = probabilityKeywordDetector;
        this.toolHintDetector = toolHintDetector;
        this.rewriteProperties = rewriteProperties;
        this.ragKnowledgeService = ragKnowledgeService;
        this.brainAgentService = brainAgentService;
        this.toolsAgentService = toolsAgentService;
        this.sseEventHelper = sseEventHelper;
        this.databaseChatMemory = databaseChatMemory;
        this.chatRunService = chatRunService;
    }

    public SseEmitter stream(ChatInputContext context, String runId) {
        SseEmitter emitter = new SseEmitter(600_000L);
        sseEventHelper.registerLifecycle(emitter);
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

        String conversationId = ConversationIds.forMode(context.userId(), context.mode(), context.chatId());
        databaseChatMemory.add(conversationId, List.of(new UserMessage(context.userMessage())));
        if (context.hasImage()) {
            databaseChatMemory.setImageUrlOnLatestUserMessage(context.chatId(), context.imageUrl());
        }

        Thread.startVirtualThread(() -> {
            try {
                // 快速路径：无图 && 无工具关键词 → 直接流式回答
                boolean canFastPath = !context.hasImage() && !toolHintDetector.detect(context.userMessage());
                if (canFastPath) {
                    log.info("快速路径激活: chatId={}", context.chatId());
                    publishStatusAsync(emitter, runId, "status", "让我想想...");
                    Flux<String> stream = brainAgentService.streamDirectAnswer(context, "");
                    streamToEmitter(emitter, stream, context, runId);
                    return;
                }

                // Step 1 & 2 并行：Intake + RAG
                CompletableFuture<IntakeAnalysisResult> intakeFuture = CompletableFuture.supplyAsync(() -> {
                    publishStatusAsync(emitter, runId, "intake_status",
                            context.hasImage()
                                    ? "宝，我正在看你的聊天截图，帮你整理一下..."
                                    : "正在整理你的问题，帮你想清楚该怎么回...");
                    return buildIntake(context);
                });
                CompletableFuture<IntakeAnalysisResult> intakeForRag = intakeFuture;
                CompletableFuture<String> ragFuture = intakeForRag.thenApplyAsync(analysis -> {
                    publishStatusAsync(emitter, runId, "rag_status", "正在查阅恋爱知识库，补充参考资料...");
                    return ragKnowledgeService.retrieveKnowledge(buildRagQuery(analysis));
                });

                IntakeAnalysisResult analysis = intakeFuture.join();
                if (analysis.hasImage()) {
                    String ocrHint = analysis.ocrText() == null || analysis.ocrText().isBlank()
                            ? "截图识别完成，正在提炼关键上下文。"
                            : "截图识别完成：" + shorten(analysis.ocrText(), 88);
                    publishStatusAsync(emitter, runId, "ocr_result", ocrHint);
                }

                String ragKnowledge = ragFuture.join();

                // Step 3: Brain 决策
                publishStatusAsync(emitter, runId, "status", "正在分析你的需求...");
                BrainDecision decision = brainAgentService.decide(context, analysis, ragKnowledge);
                publishStatusAsync(emitter, runId, "status", decision.userFacingPrelude());

                if (!decision.needsTools()) {
                    log.info("BrainAgent 决定直接回答（流式）: chatId={}", context.chatId());
                    Flux<String> stream = brainAgentService.streamDirectAnswer(context, ragKnowledge);
                    streamToEmitter(emitter, stream, context, runId);
                    return;
                }

                // Path B: Tools
                log.info("BrainAgent 激活 ToolsAgent: chatId={}", context.chatId());
                publishStatusAsync(emitter, runId, "tool_call", "宝，我帮你查点资料整理一下，稍等哈~");
                ToolsAgentResult toolResult = toolsAgentService.activate(decision, conversationId, emitter);

                publishStatusAsync(emitter, runId, "status", "资料整理好了，让我帮你总结一下...");
                Flux<String> stream = brainAgentService.streamSynthesize(
                        decision, toolResult.textResult(), toolResult.storedImages());
                streamToEmitter(emitter, stream, context, runId, toolResult.storedImages());
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

    private IntakeAnalysisResult buildIntake(ChatInputContext context) {
        if (rewriteProperties.legacyMode()) {
            log.info("Rewrite legacy-mode 启用，走旧 MultimodalIntakeService: chatId={}", context.chatId());
            return multimodalIntakeService.analyze(context);
        }
        OcrExtractionResult ocr = null;
        if (context.hasImage()) {
            ocr = ocrAgentService.extract(context.imageUrl(), context.userId(), context.userMessage());
        }
        boolean likelyNeedTools = toolHintDetector.detect(context.userMessage());
        boolean probabilityRequested = probabilityKeywordDetector.detect(context.userMessage());
        return IntakeAnalysisResult.forMainChain(context, ocr, likelyNeedTools, probabilityRequested);
    }

    private String buildRagQuery(IntakeAnalysisResult analysis) {
        String userMessage = analysis.rewrittenQuestion() == null ? "" : analysis.rewrittenQuestion();
        if (analysis.hasImage()
                && analysis.conversationSummary() != null
                && !analysis.conversationSummary().isBlank()) {
            return analysis.conversationSummary() + " / " + userMessage;
        }
        return userMessage;
    }

    private void saveAssistantMessage(String conversationId, String answer) {
        databaseChatMemory.add(conversationId, List.of(new AssistantMessage(answer)));
    }

    private String shorten(String text, int limit) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.length() <= limit ? text : text.substring(0, limit) + "...";
    }

    private Map<String, Object> terminalDonePayload(String runId, String chatId, String chatType) {
        return Map.of(
                "runId", runId,
                "chatId", chatId,
                "chatType", chatType,
                "status", "COMPLETED"
        );
    }

    private void publishStatusAsync(SseEmitter emitter, String runId, String type, String content) {
        chatRunService.recordEventAsync(runId, type, content);
        if ("intake_status".equals(type)) {
            chatRunService.markRunning(runId, content);
        }
        sseEventHelper.send(emitter, type, content);
    }

    private void streamToEmitter(SseEmitter emitter, Flux<String> flux,
                                  ChatInputContext context, String runId) {
        streamToEmitter(emitter, flux, context, runId, null);
    }

    private void streamToEmitter(SseEmitter emitter, Flux<String> flux,
                                  ChatInputContext context, String runId,
                                  List<ToolsAgentResult.StoredImageRef> storedImages) {
        StringBuilder fullContent = new StringBuilder();
        String conversationId = ConversationIds.forMode(context.userId(), context.mode(), context.chatId());

        flux.subscribe(
                chunk -> {
                    sseEventHelper.send(emitter, "content", chunk);
                    fullContent.append(chunk);
                },
                error -> {
                    log.error("流式输出失败: chatId={}, error={}", context.chatId(), error.getMessage(), error);
                    String content = fullContent.toString();
                    if (!content.isBlank()) {
                        content = fixImageUrls(content, storedImages);
                        if (storedImages != null && !storedImages.isEmpty()) {
                            sseEventHelper.send(emitter, "content_replace", content);
                        }
                        saveAssistantMessage(conversationId, content);
                        chatRunService.markCompleted(runId, content);
                    } else {
                        chatRunService.markFailed(runId, "流式生成失败: " + error.getMessage());
                    }
                    sseEventHelper.send(emitter, "error", "生成失败：" + AiErrorMessageResolver.resolve(error));
                    emitter.complete();
                },
                () -> {
                    String content = fullContent.toString();
                    content = fixImageUrls(content, storedImages);
                    if (storedImages != null && !storedImages.isEmpty()) {
                        sseEventHelper.send(emitter, "content_replace", content);
                    }
                    saveAssistantMessage(conversationId, content);
                    chatRunService.markCompleted(runId, content);
                    sseEventHelper.done(emitter, terminalDonePayload(runId, context.chatId(), "coach"));
                    emitter.complete();
                }
        );
    }

    private static final Pattern MD_IMAGE_PATTERN = Pattern.compile("!\\[([^\\]]*)\\]\\(([^)]+)\\)");

    private String fixImageUrls(String content, List<ToolsAgentResult.StoredImageRef> storedImages) {
        if (storedImages == null || storedImages.isEmpty() || content == null || content.isBlank()) {
            return content;
        }

        Set<Integer> referencedIndices = new HashSet<>();
        Matcher matcher = MD_IMAGE_PATTERN.matcher(content);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String alt = matcher.group(1);
            String url = matcher.group(2);

            boolean replaced = false;
            for (int i = 0; i < storedImages.size(); i++) {
                ToolsAgentResult.StoredImageRef img = storedImages.get(i);
                if (urlMatchesImage(url, img.fileName())) {
                    matcher.appendReplacement(sb,
                            Matcher.quoteReplacement("![" + alt + "](" + img.publicUrl() + ")"));
                    referencedIndices.add(i);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(sb);

        for (int i = 0; i < storedImages.size(); i++) {
            if (!referencedIndices.contains(i)) {
                ToolsAgentResult.StoredImageRef img = storedImages.get(i);
                sb.append("\n\n![").append(img.fileName()).append("](").append(img.publicUrl()).append(")");
            }
        }

        return sb.toString();
    }

    private boolean urlMatchesImage(String url, String fileName) {
        if (url == null || fileName == null) return false;
        if (url.contains(fileName)) return true;
        int dotIdx = fileName.lastIndexOf('.');
        if (dotIdx > 0) {
            String baseName = fileName.substring(0, dotIdx);
            if (baseName.length() >= 4 && url.contains(baseName)) return true;
        }
        return false;
    }
}
