package org.example.springai_learn.agent;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 处理工具调用的基础代理类，具体实现了 think 和 act 方法，可以用作创建实例的父类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    // 可用的工具
    private final ToolCallback[] availableTools;

    // 可选：来自 MCP 等外部来源的工具提供器
    private final ToolCallbackProvider[] extraToolProviders;

    // 保存了工具调用信息的响应
    private ChatResponse toolCallChatResponse;

    // 工具调用管理者
    private final ToolCallingManager toolCallingManager;

    // 合并后的所有工具回调列表，用于构建 ToolCallingChatOptions（需要 List<FunctionCallback>）
    private final List<FunctionCallback> allToolCallbacks;

    // 用于 ChatClient.tools() 的 ToolCallback 数组
    private final FunctionCallback[] allToolCallbacksArray;

    // 连续无工具调用的计数（用于避免空转到 maxSteps）
    private int consecutiveNoToolCalls = 0;

    // 连续无工具调用阈值（达到后输出诊断并结束）
    private int maxConsecutiveNoToolCalls = 2;

    // 本步是否进入“无工具调用快失败”分支
    private boolean failFastNoToolCallsThisStep = false;

    // 最近一次模型响应的关键信号（用于诊断输出）
    private boolean lastHasToolCalls = false;
    private int lastAssistantToolCallCount = 0;
    private String lastAssistantText = "";

    // 请求与工具输出预算（字符级估算，用于避免 DashScope 1,000,000 输入限制）
    private static final int REQUEST_CHAR_BUDGET = 900_000;
    private static final int TOOL_RESPONSE_CHAR_BUDGET = 20_000;
    private static final String TRUNCATION_SUFFIX = "... [truncated]";

    public ToolCallAgent(ToolCallback[] availableTools, ToolCallbackProvider... extraToolProviders) {
        super();
        this.availableTools = availableTools;
        this.extraToolProviders = extraToolProviders;
        // 构建工具解析器，让 ToolCallingManager 能根据 tool name 找到对应的 ToolCallback
        List<FunctionCallback> toolList = new ArrayList<>();
        toolList.addAll(List.of(availableTools));
        if (extraToolProviders != null) {
            for (ToolCallbackProvider provider : extraToolProviders) {
                if (provider == null) {
                    continue;
                }
                try {
                    FunctionCallback[] providerCallbacks = provider.getToolCallbacks();
                    for (FunctionCallback fc : providerCallbacks) {
                        toolList.add(fc);
                    }
                } catch (Exception e) {
                    log.warn("Failed to load tool callbacks from provider: {}", e.getMessage());
                }
            }
        }
        // 保存所有工具回调
        // 1. FunctionCallback[] 用于 ChatClient.tools(FunctionCallback...) 方法
        this.allToolCallbacksArray = toolList.toArray(new FunctionCallback[0]);
        // 2. List<FunctionCallback> 用于 ToolCallingChatOptions 和
        // StaticToolCallbackResolver
        this.allToolCallbacks = toolList;

        log.info("ToolCallAgent 已注册 {} 个工具: {}", allToolCallbacks.size(),
                toolList.stream()
                        .map(FunctionCallback::getName)
                        .toList());

        StaticToolCallbackResolver toolCallbackResolver = new StaticToolCallbackResolver(allToolCallbacks);
        this.toolCallingManager = ToolCallingManager.builder()
                .toolCallbackResolver(toolCallbackResolver)
                .build();
    }

    /**
     * 处理当前状态并决定下一步行动
     *
     * @return 是否需要执行行动
     */
    @Override
    public boolean think() {
        if (getNextStepPrompt() != null && !getNextStepPrompt().isEmpty()) {
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessageList().add(userMessage);
        }
        List<Message> messageList = enforceMessageBudgets(getMessageList());
        if (messageList.size() != getMessageList().size()) {
            log.warn("请求消息已根据预算裁剪: {} -> {} 条 (预算={} chars)",
                    getMessageList().size(), messageList.size(), REQUEST_CHAR_BUDGET);
            setMessageList(messageList);
        }

        try {
            log.info("Think - 工具数量: {}, 工具名称: {}",
                    allToolCallbacksArray.length,
                    Arrays.stream(allToolCallbacksArray)
                            .map(FunctionCallback::getName)
                            .toList());

            // DashScope function calling 关键：显式传递 FunctionCallbacks 到 DashScopeChatOptions
            // 仅依赖 .tools(...) 在某些情况下可能不会触发结构化 ToolCall 输出
            DashScopeChatOptions dashScopeChatOptions = DashScopeChatOptions.builder()
                    .withFunctionCallbacks(allToolCallbacks)
                    .withProxyToolCalls(Boolean.TRUE)
                    .build();

            ChatResponse chatResponse = getChatClient().prompt()
                    .messages(messageList)
                    .system(getSystemPrompt())
                    .options(dashScopeChatOptions)
                    .tools(allToolCallbacksArray)
                    .call()
                    .chatResponse();
            // 记录响应，用于 Act
            this.toolCallChatResponse = chatResponse;
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            // 输出提示信息
            String result = assistantMessage.getText();
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            if (toolCallList == null) {
                toolCallList = List.of();
            }
            boolean hasToolCalls = chatResponse.hasToolCalls();
            int assistantToolCallCount = toolCallList.size();

            // 保存信号，便于后续诊断
            this.lastHasToolCalls = hasToolCalls;
            this.lastAssistantToolCallCount = assistantToolCallCount;
            this.lastAssistantText = result == null ? "" : result;
            log.info(getName() + "的思考: " + result);
            log.info(getName() + "选择了 " + assistantToolCallCount + " 个工具来使用");
            log.info(getName() + " tool call signals: hasToolCalls={}, assistantToolCalls={} ", hasToolCalls,
                    assistantToolCallCount);
            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> String.format("工具名称：%s，参数：%s",
                            toolCall.name(),
                            toolCall.arguments()))
                    .collect(Collectors.joining("\n"));
            log.info(toolCallInfo);

            boolean shouldAct = hasToolCalls || assistantToolCallCount > 0;
            if (!shouldAct) {
                consecutiveNoToolCalls++;
                // 保存AI的回复内容，供step()方法返回
                setLastThinkingContent(result);
                if (consecutiveNoToolCalls >= maxConsecutiveNoToolCalls) {
                    failFastNoToolCallsThisStep = true;
                    return true;
                }
                // 只有不调用工具时，才记录助手消息
                getMessageList().add(assistantMessage);
                return false;
            }

            consecutiveNoToolCalls = 0;
            failFastNoToolCallsThisStep = false;
            // 需要调用工具时，无需记录助手消息，因为调用工具时会自动记录
            return true;
        } catch (Exception e) {
            log.error(getName() + "的思考过程遇到了问题: " + e.getMessage());
            getMessageList().add(
                    new AssistantMessage("处理时遇到错误: " + e.getMessage()));
            return false;
        }
    }

    /**
     * 执行工具调用并处理结果
     *
     * @return 执行结果
     */
    @Override
    public String act() {
        if (failFastNoToolCallsThisStep) {
            setState(org.example.springai_learn.agent.model.AgentState.FINISHED);
            String tools = Arrays.stream(allToolCallbacksArray)
                    .map(FunctionCallback::getName)
                    .collect(Collectors.joining(", "));
            String assistantPreview = lastAssistantText == null ? "" : lastAssistantText;
            if (assistantPreview.length() > 300) {
                assistantPreview = assistantPreview.substring(0, 300) + "...";
            }
            return "未检测到工具调用，已终止以避免空转。" +
                    "\n- 已注册工具数量: " + allToolCallbacksArray.length +
                    "\n- 工具列表: " + tools +
                    "\n- ChatResponse.hasToolCalls(): " + lastHasToolCalls +
                    "\n- AssistantMessage.getToolCalls().size(): " + lastAssistantToolCallCount +
                    "\n- 模型输出摘要: " + assistantPreview;
        }

        if (!toolCallChatResponse.hasToolCalls()) {
            return "没有工具调用";
        }
        // 构建包含工具回调的 ChatOptions
        ToolCallingChatOptions chatOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(allToolCallbacks)
                .internalToolExecutionEnabled(false)
                .build();
        // 调用工具
        Prompt prompt = new Prompt(getMessageList(), chatOptions);
        ToolExecutionResult toolExecutionResult;
        try {
            toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        } catch (Exception e) {
            log.error("工具执行失败: {}", e.getMessage(), e);
            return "工具执行失败: " + e.getMessage();
        }
        // 记录消息上下文，conversationHistory 已经包含了助手消息和工具调用返回的结果
        List<Message> history = enforceMessageBudgets(toolExecutionResult.conversationHistory());
        setMessageList(history);

        // 健壮提取 ToolResponseMessage：从历史中查找最新的 ToolResponseMessage，而非假设最后一条一定是
        ToolResponseMessage toolResponseMessage = null;
        for (int i = history.size() - 1; i >= 0; i--) {
            if (history.get(i) instanceof ToolResponseMessage trm) {
                toolResponseMessage = trm;
                break;
            }
        }

        String results;
        boolean hasTerminate = false;
        if (toolResponseMessage != null) {
            results = toolResponseMessage.getResponses().stream()
                    .map(response -> {
                        // Check if file was created and send SSE notification
                        checkAndSendFileEvent(response.name(), response.responseData());
                        return "工具 " + response.name() + " 完成了它的任务！结果: " + response.responseData();
                    })
                    .collect(Collectors.joining("\n"));
            // 检测是否调用了terminate工具
            hasTerminate = toolResponseMessage.getResponses().stream()
                    .anyMatch(response -> "doTerminate".equals(response.name()));
        } else {
            results = "工具执行完成，但未找到工具响应消息";
            log.warn(results);
        }
        log.info(results);

        if (hasTerminate) {
            log.info("检测到terminate工具调用，设置代理状态为FINISHED");
            setState(org.example.springai_learn.agent.model.AgentState.FINISHED);
        }

        return results;
    }

    private List<Message> enforceMessageBudgets(List<Message> messages) {
        List<Message> bounded = new ArrayList<>();
        int total = 0;
        int dropped = 0;

        for (int i = messages.size() - 1; i >= 0; i--) {
            Message original = messages.get(i);
            Message candidate = clampToolResponse(original);
            int len = estimateLength(candidate);
            // 始终保留最新一条，即使超过预算
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
            log.warn("工具输出过长，已截断: {} (每条上限 {} chars)",
                    responses.stream().map(ToolResponseMessage.ToolResponse::name).toList(),
                    TOOL_RESPONSE_CHAR_BUDGET);
            return new ToolResponseMessage(responses);
        }
        return message;
    }

    private int estimateLength(Message message) {
        if (message instanceof ToolResponseMessage toolResponseMessage) {
            return toolResponseMessage.getResponses().stream()
                    .mapToInt(r -> safeLen(r.responseData()))
                    .sum();
        }
        if (message instanceof AssistantMessage assistantMessage) {
            return safeLen(assistantMessage.getText());
        }
        if (message instanceof UserMessage userMessage) {
            return safeLen(userMessage.getText());
        }
        return 0;
    }

    private int safeLen(String text) {
        return text == null ? 0 : text.length();
    }

    /**
     * Parse tool result and send file_created event if applicable
     */
    private void checkAndSendFileEvent(String toolName, String result) {
        log.info("checkAndSendFileEvent called: toolName={}, currentEmitter={}, resultPreview={}",
                toolName,
                currentEmitter != null ? "available" : "NULL",
                result != null ? result.substring(0, Math.min(100, result.length())) : "null");

        if (currentEmitter == null) {
            log.warn("currentEmitter is null, cannot send file_created event");
            return;
        }
        if (result == null) {
            return;
        }

        // 工具返回的结果可能被引号包裹，需要去除
        String cleanResult = result.trim();
        if (cleanResult.startsWith("\"") && cleanResult.endsWith("\"") && cleanResult.length() > 1) {
            cleanResult = cleanResult.substring(1, cleanResult.length() - 1);
            log.info("Stripped quotes from result, cleanResult starts with: {}",
                    cleanResult.substring(0, Math.min(50, cleanResult.length())));
        }

        try {
            String type = null;
            String filePath = null;

            if ("generatePDF".equals(toolName) && cleanResult.startsWith("PDF generated successfully")) {
                // Format: "PDF generated successfully to: D:\path\to.pdf (size bytes)..."
                int startIndex = cleanResult.indexOf("to: ");
                if (startIndex != -1) {
                    startIndex += 4;
                    int endIndex = cleanResult.indexOf(" (", startIndex);
                    if (endIndex == -1)
                        endIndex = cleanResult.length();
                    filePath = cleanResult.substring(startIndex, endIndex).trim();
                    type = "pdf";
                    log.info("Detected PDF generation: {}", filePath);
                }
            } else if (("downloadResource".equals(toolName) || "downloadImage".equals(toolName))
                    && cleanResult.startsWith("Resource downloaded successfully")) {
                // Format: "Resource downloaded successfully to: D:\path\to.jpg (Content-Type:
                // ...)"
                int startIndex = cleanResult.indexOf("to: ");
                if (startIndex != -1) {
                    startIndex += 4;
                    int endIndex = cleanResult.indexOf(" (", startIndex);
                    if (endIndex == -1)
                        endIndex = cleanResult.length();
                    filePath = cleanResult.substring(startIndex, endIndex).trim();
                    type = "image";
                    log.info("Detected image download: {}", filePath);
                }
            } else if ("downloadImage".equals(toolName) && cleanResult.startsWith("Image downloaded successfully")) {
                // MCP ImageSearchTool format: "Image downloaded successfully to: D:\path\to.jpg
                // (status=..."
                int startIndex = cleanResult.indexOf("to: ");
                if (startIndex != -1) {
                    startIndex += 4;
                    int endIndex = cleanResult.indexOf(" (", startIndex);
                    if (endIndex == -1)
                        endIndex = cleanResult.length();
                    filePath = cleanResult.substring(startIndex, endIndex).trim();
                    type = "image";
                    log.info("Detected MCP image download: {}", filePath);
                }
            }

            if (type != null && filePath != null) {
                File file = new File(filePath);
                String fileName = file.getName();
                log.info("Checking file existence: {} exists={}", filePath, file.exists());

                // Check if file actually exists
                if (file.exists()) {
                    // Construct URL relative to backend
                    String urlType = type.equals("pdf") ? "pdf" : "download";
                    String url = "/api/files/" + urlType + "/" + fileName;

                    log.info("Sending file_created event: type={}, name={}, path={}, url={}", type, fileName, filePath,
                            url);
                    sendFileCreated(currentEmitter, type, fileName, filePath, url);
                    log.info("file_created event sent successfully");
                } else {
                    log.warn("File does not exist: {}", filePath);
                }
            } else {
                log.debug("No file detected in tool result: toolName={}", toolName);
            }
        } catch (Exception e) {
            log.warn("Failed to parse file path from tool result: {}", e.getMessage(), e);
        }
    }

}
