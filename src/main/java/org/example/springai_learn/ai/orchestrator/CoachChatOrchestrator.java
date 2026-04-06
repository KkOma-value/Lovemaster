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

        // 立即持久化用户消息，避免刷新页面时丢失提问
        String conversationId = ConversationIds.forMode(context.userId(), context.mode(), context.chatId());
        databaseChatMemory.add(conversationId, List.of(new UserMessage(context.userMessage())));
        if (context.hasImage()) {
            databaseChatMemory.setImageUrlOnLatestUserMessage(context.chatId(), context.imageUrl());
        }

        Thread.startVirtualThread(() -> {
            try {
                // ── 快速路径：无图片 && 不需要工具 → 直接流式回答 ──
                boolean canFastPath = !context.hasImage() && !heuristicToolNeed(context.userMessage());
                if (canFastPath) {
                    log.info("快速路径激活: chatId={}", context.chatId());
                    publishStatusAsync(emitter, runId, "status", "让我想想...");

                    Flux<String> stream = brainAgentService.streamDirectAnswer(
                            context, "");
                    streamToEmitter(emitter, stream, context, runId);
                    return;
                }

                // ── Step 1 & 2 并行：Intake + RAG ──
                CompletableFuture<IntakeAnalysisResult> intakeFuture = CompletableFuture.supplyAsync(() -> {
                    publishStatusAsync(emitter, runId, "intake_status",
                            context.hasImage()
                                    ? "宝，我正在看你的聊天截图，帮你整理一下..."
                                    : "正在整理你的问题，帮你想清楚该怎么回...");
                    return multimodalIntakeService.analyze(context);
                });
                CompletableFuture<String> ragFuture = CompletableFuture.supplyAsync(() -> {
                    publishStatusAsync(emitter, runId, "rag_status", "正在查阅恋爱知识库，补充参考资料...");
                    return ragKnowledgeService.retrieveKnowledge(context.userMessage());
                });

                // 等待 Intake 完成
                IntakeAnalysisResult analysis = intakeFuture.join();
                // 处理 intake 结果的 SSE 推送（OCR、rewrite 等）
                if (analysis.hasImage()) {
                    String ocrHint = analysis.ocrText() == null || analysis.ocrText().isBlank()
                            ? "截图识别完成，正在提炼关键上下文。"
                            : "截图识别完成：" + shorten(analysis.ocrText(), 88);
                    publishStatusAsync(emitter, runId, "ocr_result", ocrHint);
                }
                publishStatusAsync(emitter, runId, "rewrite_result",
                        "我已经把任务整理成更清晰的问题：" + shorten(analysis.rewrittenQuestion(), 96));

                // 等待 RAG 完成
                String ragKnowledge = ragFuture.join();

                // ── Step 3: Brain 决策 — 判断是否需要工具 ──
                publishStatusAsync(emitter, runId, "status", "正在分析你的需求...");
                BrainDecision decision = brainAgentService.decide(context, analysis, ragKnowledge);
                publishStatusAsync(emitter, runId, "status", decision.userFacingPrelude());

                if (!decision.needsTools()) {
                    // ── Path A: Brain 直接回答（流式） ──
                    log.info("BrainAgent 决定直接回答（流式）: chatId={}", context.chatId());
                    Flux<String> stream = brainAgentService.streamDirectAnswer(
                            context, ragKnowledge);
                    streamToEmitter(emitter, stream, context, runId);
                    return;
                }

                // ── Path B: Brain 激活 ToolsAgent ──
                log.info("BrainAgent 激活 ToolsAgent: chatId={}", context.chatId());
                publishStatusAsync(emitter, runId, "tool_call", "宝，我帮你查点资料整理一下，稍等哈~");

                // Step 4: ToolsAgent 执行工具任务（同步，file_created 事件通过 emitter 推送）
                ToolsAgentResult toolResult = toolsAgentService.activate(decision, conversationId, emitter);

                // Step 5: Brain 综合工具结果，流式生成最终回答（包含已存储图片 URL）
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

    /**
     * 启发式判断是否需要工具调用。
     * 基于关键词匹配，快速路径判断使用。
     */
    private boolean heuristicToolNeed(String userMessage) {
        if (userMessage == null) return false;
        String text = userMessage.toLowerCase();
        return text.contains("计划") || text.contains("方案") || text.contains("帮我查")
                || text.contains("搜索") || text.contains("推荐") || text.contains("攻略")
                || text.contains("景点") || text.contains("地点") || text.contains("餐厅")
                || text.contains("旅游") || text.contains("旅行") || text.contains("照片")
                || text.contains("图片") || text.contains("看看") || text.contains("整理")
                || text.contains("生成") || text.contains("pdf") || text.contains("文档");
    }

    /**
     * 异步推送状态事件，使用 ChatRunService 的 recordEventAsync。
     */
    private void publishStatusAsync(SseEmitter emitter, String runId, String type, String content) {
        chatRunService.recordEventAsync(runId, type, content);
        if ("intake_status".equals(type)) {
            chatRunService.markRunning(runId, content);
        }
        sseEventHelper.send(emitter, type, content);
    }

    /**
     * 将 Flux<String> 流式内容桥接到 SseEmitter（无图片后处理）。
     */
    private void streamToEmitter(SseEmitter emitter, Flux<String> flux,
                                  ChatInputContext context, String runId) {
        streamToEmitter(emitter, flux, context, runId, null);
    }

    /**
     * 将 Flux<String> 流式内容桥接到 SseEmitter。
     * 每个 chunk 作为一个 "content" SSE 事件推送。
     * 流完成后对图片 URL 做后处理，再保存消息并标记完成。
     */
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
                    // 将修正后的完整内容回传前端，替换流式过程中可能损坏的图片 URL
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

    // ---- 图片 URL 后处理 ----

    private static final Pattern MD_IMAGE_PATTERN = Pattern.compile("!\\[([^\\]]*)\\]\\(([^)]+)\\)");

    /**
     * 修复 LLM 输出中的图片 URL，并补全遗漏的图片。
     * <p>
     * LLM 经常无法精确复制长 URL（尤其是 Supabase 路径），导致前端无法加载。
     * 此方法通过文件名匹配，将错误 URL 替换为 storedImages 中的正确 URL。
     * 未被 LLM 引用的图片会追加到内容末尾。
     */
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
                // 通过文件名子串匹配（LLM 可能截断路径但保留文件名）
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

        // 追加 LLM 遗漏的图片
        for (int i = 0; i < storedImages.size(); i++) {
            if (!referencedIndices.contains(i)) {
                ToolsAgentResult.StoredImageRef img = storedImages.get(i);
                sb.append("\n\n![").append(img.fileName()).append("](").append(img.publicUrl()).append(")");
            }
        }

        return sb.toString();
    }

    /**
     * 判断 LLM 输出的 URL 是否匹配某个已存储图片。
     * 使用文件名匹配（去掉扩展名的主体部分），兼容 LLM 截断/修改路径的情况。
     */
    private boolean urlMatchesImage(String url, String fileName) {
        if (url == null || fileName == null) return false;
        // 完整文件名匹配
        if (url.contains(fileName)) return true;
        // 去掉扩展名的主体匹配（LLM 可能改了扩展名）
        int dotIdx = fileName.lastIndexOf('.');
        if (dotIdx > 0) {
            String baseName = fileName.substring(0, dotIdx);
            if (baseName.length() >= 4 && url.contains(baseName)) return true;
        }
        return false;
    }
}
