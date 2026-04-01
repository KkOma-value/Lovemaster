package org.example.springai_learn.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.ChatMemory.DatabaseChatMemory;
import org.example.springai_learn.advisor.MyLoggerAdvisor;
import org.example.springai_learn.ai.context.BrainDecision;
import org.example.springai_learn.ai.context.ToolsAgentResult;
import org.example.springai_learn.auth.service.ConversationImageStorageService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ToolsAgent — 工具供给与执行代理。
 * <p>
 * 职责：
 * 当 BrainAgent 判断需要工具时，由 Orchestrator 激活本服务。
 * 使用 ChatClient + ToolCallingManager 直接执行工具调用循环，
 * 同步执行并返回结果给 BrainAgent。
 * <p>
 * 关系：BrainAgent.decide() → 需要工具 → ToolsAgent.activate() → 工具执行 → 结果返回 → BrainAgent.synthesize()
 */
@Service
@Slf4j
public class ToolsAgentService {

    private static final int MAX_TOOL_ITERATIONS = 20;
    private static final int MAX_CONSECUTIVE_NO_TOOL_CALLS = 2;
    private static final int REQUEST_CHAR_BUDGET = 900_000;
    private static final int TOOL_RESPONSE_CHAR_BUDGET = 20_000;
    private static final String TRUNCATION_SUFFIX = "... [truncated]";
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s,\\\\]+");

    private static final String SYSTEM_PROMPT = """
            You are ToolsAgent, an autonomous tool-execution agent that completes tasks delegated by BrainAgent.
            You have various tools at your disposal that you can call upon to efficiently complete complex requests.

            IMPORTANT RULES:
            1. You are FULLY AUTONOMOUS - never ask the user for confirmation, clarification, or approval.
            2. Make all decisions yourself based on reasonable assumptions.
            3. If information is missing, make sensible default choices and proceed.
            4. Execute tasks directly using tools - do not just describe what you would do.
            5. Only output the final results to the user, not intermediate questions.
            6. Always call at least one tool per step until the task is complete.
            7. When the task requests photos, images, or visual references, do not stop after finding image URLs.
               You must continue with searchImage + downloadResource for the strongest 2-3 images per subject so cloud image artifacts are created.
            8. Prefer persisted cloud image artifacts over raw external URLs whenever the request explicitly asks for images, attachments, or downloadable materials.

            RESPONSE FORMAT RULES:
            - NEVER use template syntax like {{}} or {variable} in your responses.
            - Always respond in clean, natural language without any code syntax or markup artifacts.
            - When using doTerminate, provide a clear, well-formatted summary for the user.
            - Do NOT include raw JSON, tool names, or technical details in user-facing text.
            """;

    private static final String NEXT_STEP_PROMPT = """
            Analyze the current state and immediately take action using the most appropriate tool.
            Do NOT ask the user any questions - make autonomous decisions.
            Do NOT explain what you plan to do - just do it by calling tools.
            If you have completed the task, call the `doTerminate` tool with a summary of results.
            If the task includes images, verify that you have already created the needed downloaded image artifacts before terminating.
            If there are more steps needed, call the next required tool now.
            """;

    private final ToolCallback[] allTools;
    private final ChatModel toolsModel;
    private final DatabaseChatMemory databaseChatMemory;
    private final ConversationImageStorageService conversationImageStorageService;

    @Autowired(required = false)
    private ToolCallbackProvider mcpToolCallbackProvider;

    public ToolsAgentService(
            ToolCallback[] allTools,
            @Qualifier("toolsModel") ChatModel toolsModel,
            DatabaseChatMemory databaseChatMemory,
            ConversationImageStorageService conversationImageStorageService) {
        this.allTools = allTools;
        this.toolsModel = toolsModel;
        this.databaseChatMemory = databaseChatMemory;
        this.conversationImageStorageService = conversationImageStorageService;

        // 合并所有工具回调（注册工具 + MCP 工具在 activate 时延迟合并）
        List<FunctionCallback> toolList = new ArrayList<>(List.of(allTools));

        log.info("ToolsAgentService 已注册 {} 个工具: {}",
                toolList.size(),
                toolList.stream().map(FunctionCallback::getName).toList());
    }

    /**
     * 激活 ToolsAgent，同步执行 Brain 分配的任务。
     *
     * @param decision       BrainAgent 的决策
     * @param conversationId 会话复合 ID
     * @param emitter        SSE 发送器（file_created 等事件通过此推送）
     * @return 工具执行的结果文本
     */
    public ToolsAgentResult activate(BrainDecision decision, String conversationId, SseEmitter emitter) {
        log.info("ToolsAgent 被激活: conversationId={}, taskPrompt={}",
                conversationId, shorten(decision.toolTaskPrompt(), 120));

        databaseChatMemory.ensureConversationExists(conversationId);

        // 收集已存储图片（供 Brain 合成时引用）
        List<ToolsAgentResult.StoredImageRef> collectedImages = new ArrayList<>();

        // 构建合并工具列表（包含 MCP 工具）
        List<FunctionCallback> mergedTools = buildMergedToolList();

        // 构建 ToolCallingManager（使用合并后的工具列表）
        StaticToolCallbackResolver resolver = new StaticToolCallbackResolver(mergedTools);
        ToolCallingManager mergedToolCallingManager = ToolCallingManager.builder()
                .toolCallbackResolver(resolver)
                .build();

        FunctionCallback[] mergedToolsArray = mergedTools.toArray(new FunctionCallback[0]);

        // 构建 ChatClient
        ChatClient chatClient = ChatClient.builder(toolsModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();

        // 加载历史消息
        List<Message> messages = loadHistory(conversationId);
        messages.add(new UserMessage(decision.toolTaskPrompt()));

        String lastResult = "";
        int consecutiveNoToolCalls = 0;

        for (int i = 0; i < MAX_TOOL_ITERATIONS; i++) {
            int stepNumber = i + 1;
            log.info("ToolsAgent 执行步骤 {}/{}", stepNumber, MAX_TOOL_ITERATIONS);

            // 添加 next-step prompt（第一步以外）
            if (i > 0) {
                messages.add(new UserMessage(NEXT_STEP_PROMPT));
            }

            // 预算裁剪
            messages = enforceMessageBudgets(messages);

            // 构建工具选项
            ToolCallingChatOptions chatOptions = ToolCallingChatOptions.builder()
                    .toolCallbacks(mergedTools)
                    .internalToolExecutionEnabled(false)
                    .build();

            // 调用模型
            ChatResponse chatResponse;
            try {
                chatResponse = chatClient.prompt()
                        .messages(messages)
                        .system(SYSTEM_PROMPT)
                        .options(chatOptions)
                        .tools(mergedToolsArray)
                        .call()
                        .chatResponse();
            } catch (Exception e) {
                log.error("ToolsAgent 模型调用失败: {}", e.getMessage(), e);
                lastResult = "工具执行失败: " + e.getMessage();
                break;
            }

            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            boolean hasToolCalls = chatResponse.hasToolCalls();
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            if (toolCallList == null) {
                toolCallList = List.of();
            }

            log.info("ToolsAgent 思考: {}", shorten(assistantMessage.getText(), 200));
            log.info("ToolsAgent tool calls: hasToolCalls={}, count={}", hasToolCalls, toolCallList.size());

            if (!hasToolCalls && toolCallList.isEmpty()) {
                consecutiveNoToolCalls++;
                messages.add(assistantMessage);

                if (consecutiveNoToolCalls >= MAX_CONSECUTIVE_NO_TOOL_CALLS) {
                    log.warn("ToolsAgent 连续 {} 步无工具调用，终止", consecutiveNoToolCalls);
                    lastResult = assistantMessage.getText() != null ? assistantMessage.getText() : "";
                    break;
                }
                continue;
            }

            consecutiveNoToolCalls = 0;

            // 执行工具调用
            Prompt prompt = new Prompt(messages, chatOptions);
            ToolExecutionResult toolExecutionResult;
            try {
                toolExecutionResult = mergedToolCallingManager.executeToolCalls(prompt, chatResponse);
            } catch (Exception e) {
                log.error("ToolsAgent 工具执行失败: {}", e.getMessage(), e);
                lastResult = "工具执行失败: " + e.getMessage();
                break;
            }

            messages = new ArrayList<>(toolExecutionResult.conversationHistory());

            // 从历史中查找最新的 ToolResponseMessage
            ToolResponseMessage toolResponseMessage = null;
            for (int j = messages.size() - 1; j >= 0; j--) {
                if (messages.get(j) instanceof ToolResponseMessage trm) {
                    toolResponseMessage = trm;
                    break;
                }
            }

            if (toolResponseMessage != null) {
                boolean hasTerminate = false;
                for (ToolResponseMessage.ToolResponse response : toolResponseMessage.getResponses()) {
                    checkAndSendFileEvent(response.name(), response.responseData(), emitter, conversationId, collectedImages);
                    log.info("工具 {} 执行完成", response.name());
                    if ("doTerminate".equals(response.name())) {
                        hasTerminate = true;
                        lastResult = response.responseData();
                    }
                }
                if (hasTerminate) {
                    log.info("ToolsAgent 检测到 doTerminate，结束执行");
                    break;
                }
            }

            // 如果是最后一轮，取助手消息作为结果
            if (i == MAX_TOOL_ITERATIONS - 1) {
                lastResult = assistantMessage.getText() != null ? assistantMessage.getText() : "";
                log.warn("ToolsAgent 达到最大步数 {}", MAX_TOOL_ITERATIONS);
            }
        }

        log.info("ToolsAgent 执行完成: result={}, images={}", shorten(lastResult, 200), collectedImages.size());
        return new ToolsAgentResult(lastResult, collectedImages);
    }

    // ---- 历史消息管理 ----

    private List<Message> loadHistory(String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            List<Message> history = databaseChatMemory.get(conversationId, 20);
            if (!history.isEmpty()) {
                log.info("从 ChatMemory 加载了 {} 条历史消息: conversationId={}", history.size(), conversationId);
                return new ArrayList<>(history);
            }
        } catch (Exception e) {
            log.warn("加载历史消息失败: conversationId={}, error={}", conversationId, e.getMessage());
        }
        return new ArrayList<>();
    }

    private void saveHistory(String conversationId, List<Message> messages) {
        if (conversationId == null || conversationId.isEmpty()) {
            return;
        }
        try {
            List<Message> messagesToSave = new ArrayList<>();
            for (Message msg : messages) {
                if (msg instanceof UserMessage userMsg) {
                    String text = userMsg.getText();
                    // 过滤掉内部提示（NEXT_STEP_PROMPT）
                    if (text != null && !text.trim().startsWith("Analyze the current state")) {
                        messagesToSave.add(msg);
                    }
                } else if (msg instanceof AssistantMessage assistantMsg) {
                    String text = assistantMsg.getText();
                    if (text != null && !text.trim().isEmpty()) {
                        messagesToSave.add(msg);
                    }
                }
            }
            if (!messagesToSave.isEmpty()) {
                databaseChatMemory.replaceMessages(conversationId, messagesToSave);
                log.info("已保存 {} 条消息到 ChatMemory: conversationId={}", messagesToSave.size(), conversationId);
            }
        } catch (Exception e) {
            log.error("保存消息到 ChatMemory 失败: conversationId={}, error={}", conversationId, e.getMessage(), e);
        }
    }

    // ---- 消息预算控制 ----

    private List<Message> enforceMessageBudgets(List<Message> messages) {
        List<Message> bounded = new ArrayList<>();
        int total = 0;
        int dropped = 0;

        for (int i = messages.size() - 1; i >= 0; i--) {
            Message original = messages.get(i);
            Message candidate = clampToolResponse(original);
            int len = estimateLength(candidate);
            if (!bounded.isEmpty() && total + len > REQUEST_CHAR_BUDGET) {
                dropped++;
                continue;
            }
            bounded.add(0, candidate);
            total += len;
        }

        if (dropped > 0) {
            log.warn("为满足请求预算丢弃旧消息 {} 条，总计长度 {} / {}", dropped, total, REQUEST_CHAR_BUDGET);
        }
        return bounded;
    }

    private Message clampToolResponse(Message message) {
        if (!(message instanceof ToolResponseMessage toolResponseMessage)) {
            return message;
        }

        boolean truncated = false;
        List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>();
        for (ToolResponseMessage.ToolResponse response : toolResponseMessage.getResponses()) {
            String data = response.responseData();
            if (data != null && data.length() > TOOL_RESPONSE_CHAR_BUDGET) {
                truncated = true;
                data = data.substring(0, TOOL_RESPONSE_CHAR_BUDGET) + TRUNCATION_SUFFIX;
            }
            responses.add(new ToolResponseMessage.ToolResponse(response.id(), response.name(), data));
        }

        if (truncated) {
            log.warn("工具输出过长，已截断: {}",
                    responses.stream().map(ToolResponseMessage.ToolResponse::name).toList());
            return new ToolResponseMessage(responses);
        }
        return message;
    }

    private int estimateLength(Message message) {
        if (message instanceof ToolResponseMessage trm) {
            return trm.getResponses().stream()
                    .mapToInt(r -> safeLen(r.responseData()))
                    .sum();
        }
        if (message instanceof AssistantMessage am) {
            return safeLen(am.getText());
        }
        if (message instanceof UserMessage um) {
            return safeLen(um.getText());
        }
        return 0;
    }

    private int safeLen(String text) {
        return text == null ? 0 : text.length();
    }

    // ---- 工具列表合并 ----

    private List<FunctionCallback> buildMergedToolList() {
        List<FunctionCallback> merged = new ArrayList<>(List.of(allTools));
        if (mcpToolCallbackProvider != null) {
            try {
                FunctionCallback[] mcpCallbacks = mcpToolCallbackProvider.getToolCallbacks();
                for (FunctionCallback cb : mcpCallbacks) {
                    if ("searchImage".equals(cb.getName())) {
                        log.info("跳过 MCP 工具 {}，优先使用主应用中的图片工具", cb.getName());
                        continue;
                    }
                    merged.add(cb);
                }
                log.info("MCP 工具已合并，新增 {} 个", mcpCallbacks.length);
            } catch (Exception e) {
                log.warn("加载 MCP 工具失败: {}", e.getMessage());
            }
        }
        return merged;
    }

    // ---- 文件事件 SSE 推送 ----

    private void checkAndSendFileEvent(String toolName, String result, SseEmitter emitter,
                                       String conversationId, List<ToolsAgentResult.StoredImageRef> collectedImages) {
        if (emitter == null || result == null) {
            return;
        }

        String chatId = extractChatId(conversationId);

        String cleanResult = result.trim();
        if (cleanResult.startsWith("\"") && cleanResult.endsWith("\"") && cleanResult.length() > 1) {
            cleanResult = cleanResult.substring(1, cleanResult.length() - 1);
        }
        cleanResult = cleanResult.replace("\\n", "\n");

        // searchImage 仅提供候选 URL，不代表已产出可下载工件。
        if ("searchImage".equals(toolName)) {
            List<String> imageUrls = extractImageUrls(cleanResult);
            if (!imageUrls.isEmpty()) {
                log.info("检测到 searchImage 返回 {} 个候选图片 URL，等待后续下载后再发送 file_created", imageUrls.size());
            }
        }

        try {
            String type = null;
            String filePath = null;
            String sourceUrl = extractMetadataValue(cleanResult, "sourceUrl");
            String fileName = extractMetadataValue(cleanResult, "fileName");

            if ("generatePDF".equals(toolName) && cleanResult.startsWith("PDF generated successfully")) {
                int startIndex = cleanResult.indexOf("to: ");
                if (startIndex != -1) {
                    startIndex += 4;
                    int endIndex = cleanResult.indexOf(" (", startIndex);
                    if (endIndex == -1) endIndex = cleanResult.length();
                    filePath = cleanResult.substring(startIndex, endIndex).trim();
                    type = "pdf";
                }
            } else if ("downloadResource".equals(toolName) && cleanResult.startsWith("Resource downloaded successfully")) {
                type = "image";
            } else if ("downloadImage".equals(toolName) && cleanResult.startsWith("Image downloaded successfully")) {
                type = "image";
                filePath = extractMetadataValue(cleanResult, "localPath");
            }

            if ("image".equals(type) && sourceUrl != null && fileName != null) {
                ConversationImageStorageService.StoredConversationImage storedImage =
                        conversationImageStorageService.storeRemoteImage(chatId, sourceUrl, fileName);
                log.info("发送 file_created 事件: type=image, name={}, url={}",
                        storedImage.getFileName(), storedImage.getPublicUrl());
                sendFileCreated(emitter, "image", storedImage.getFileName(),
                        storedImage.getStoragePath(), storedImage.getPublicUrl());
                if (collectedImages != null) {
                    collectedImages.add(new ToolsAgentResult.StoredImageRef(
                            storedImage.getFileName(), storedImage.getPublicUrl()));
                }
                return;
            }

            if (type != null && filePath != null) {
                File file = new File(filePath);
                if (file.exists()) {
                    String resolvedFileName = file.getName();
                    String urlType = type.equals("pdf") ? "pdf" : "download";
                    String publicUrl = "/api/files/" + urlType + "/" + resolvedFileName;
                    log.info("发送 file_created 事件: type={}, name={}, url={}", type, resolvedFileName, publicUrl);
                    sendFileCreated(emitter, type, resolvedFileName, filePath, publicUrl);
                }
            }
        } catch (Exception e) {
            log.warn("解析或持久化文件事件失败: {}", e.getMessage());
        }
    }

    private String extractChatId(String conversationId) {
        String[] parts = conversationId.split(":");
        return parts.length >= 3 ? parts[2] : conversationId;
    }

    private void sendFileCreated(SseEmitter emitter, String fileType, String fileName,
                                  String filePath, String fileUrl) {
        try {
            String json = String.format(
                    "{\"type\":\"file_created\",\"content\":\"%s\",\"data\":{\"type\":\"%s\",\"name\":\"%s\",\"path\":\"%s\",\"url\":\"%s\"}}",
                    escape(fileName),
                    escape(fileType),
                    escape(fileName),
                    escape(filePath),
                    escape(fileUrl != null ? fileUrl : ""));
            emitter.send(json);
        } catch (Exception e) {
            if (isCompletedEmitterError(e)) {
                log.debug("SSE 已结束，忽略 file_created 事件: {}", e.getMessage());
                return;
            }
            log.warn("发送 file_created 事件失败: {}", e.getMessage());
        }
    }

    private String escape(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private List<String> extractImageUrls(String text) {
        if (text == null || text.isBlank()) return List.of();
        Matcher matcher = URL_PATTERN.matcher(text);
        Set<String> urls = new LinkedHashSet<>();
        while (matcher.find()) {
            String url = stripTrailingPunctuation(matcher.group());
            if (url != null && !url.isBlank()) {
                urls.add(url);
            }
        }
        return new ArrayList<>(urls);
    }

    private String stripTrailingPunctuation(String url) {
        if (url == null) return null;
        String cleaned = url;
        while (!cleaned.isEmpty()) {
            char last = cleaned.charAt(cleaned.length() - 1);
            if (last == ')' || last == ']' || last == '}' || last == ',' || last == '.' || last == ';') {
                cleaned = cleaned.substring(0, cleaned.length() - 1);
            } else {
                break;
            }
        }
        return cleaned;
    }

    private String extractMetadataValue(String text, String key) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String prefix = key + ":";
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(prefix)) {
                String value = trimmed.substring(prefix.length()).trim();
                return value.isEmpty() ? null : value;
            }
        }
        return null;
    }

    private boolean isCompletedEmitterError(Exception e) {
        String message = e.getMessage();
        return message != null && message.contains("ResponseBodyEmitter has already completed");
    }

    private String shorten(String text, int limit) {
        if (text == null || text.isBlank()) return "";
        return text.length() <= limit ? text : text.substring(0, limit) + "...";
    }
}
