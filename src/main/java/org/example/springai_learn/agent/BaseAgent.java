package org.example.springai_learn.agent;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.agent.model.AgentState;
import org.jsoup.internal.StringUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 抽象基础代理类，用于管理代理状态和执行流程。
 * 
 * 提供状态转换、内存管理和基于步骤的执行循环的基础功能。
 * 子类必须实现step方法。
 */
@Data
@Slf4j
public abstract class BaseAgent {

    // 核心属性
    private String name;

    // 提示
    private String systemPrompt;
    private String nextStepPrompt;

    // 状态
    private AgentState state = AgentState.IDLE;

    // 执行控制
    private int maxSteps = 10;
    private int currentStep = 0;

    // LLM
    private ChatClient chatClient;

    // Memory（需要自主维护会话上下文）
    private List<Message> messageList = new ArrayList<>();

    // NOTE: 显式 getter/setter 用于在 IDE 未启用 Lombok 注解处理时仍可编译运行。
    // Maven 构建会正常生成 Lombok 方法，但 IDE 增量编译可能不会，从而导致运行时类被编译为“Unresolved compilation
    // problems”。
    // 当前的 SSE Emitter (用于在 act() 中发送额外事件)
    protected SseEmitter currentEmitter;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SseEmitter getCurrentEmitter() {
        return currentEmitter;
    }

    public void setCurrentEmitter(SseEmitter currentEmitter) {
        this.currentEmitter = currentEmitter;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getNextStepPrompt() {
        return nextStepPrompt;
    }

    public void setNextStepPrompt(String nextStepPrompt) {
        this.nextStepPrompt = nextStepPrompt;
    }

    public AgentState getState() {
        return state;
    }

    public void setState(AgentState state) {
        this.state = state;
    }

    public int getMaxSteps() {
        return maxSteps;
    }

    public void setMaxSteps(int maxSteps) {
        this.maxSteps = maxSteps;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(int currentStep) {
        this.currentStep = currentStep;
    }

    public ChatClient getChatClient() {
        return chatClient;
    }

    public void setChatClient(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public List<Message> getMessageList() {
        return messageList;
    }

    public void setMessageList(List<Message> messageList) {
        this.messageList = messageList;
    }

    /**
     * 运行代理
     * 
     * @param userPrompt 用户提示词
     * @return 执行结果
     */
    public String run(String userPrompt) {
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("Cannot run agent from state: " + this.state);
        }
        if (StringUtil.isBlank(userPrompt)) {
            throw new RuntimeException("Cannot run agent with empty user prompt");
        }
        // 更改状态
        state = AgentState.RUNNING;
        // 记录消息上下文
        messageList.add(new UserMessage(userPrompt));
        // 保存结果列表
        List<String> results = new ArrayList<>();
        try {
            for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("Executing step " + stepNumber + "/" + maxSteps);
                // 单步执行
                String stepResult = step();
                String result = "Step " + stepNumber + ": " + stepResult;
                results.add(result);
            }
            // 检查是否超出步骤限制
            if (currentStep >= maxSteps) {
                state = AgentState.FINISHED;
                results.add("Terminated: Reached max steps (" + maxSteps + ")");
            }
            return String.join("\n", results);
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("Error executing agent", e);
            return "执行错误" + e.getMessage();
        } finally {
            // 清理资源
            this.cleanup();
        }
    }

    /**
     * 执行单个步骤
     * 
     * @return 步骤执行结果
     */
    public abstract String step();

    /**
     * 清理资源
     */
    protected void cleanup() {
        // 子类可以重写此方法来清理资源
    }

    /**
     * 运行代理（流式输出）
     *
     * @param userPrompt 用户提示词
     * @return SseEmitter实例
     */
    public SseEmitter runStream(String userPrompt) {
        // 创建SseEmitter，设置较长的超时时间
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时
        this.currentEmitter = emitter;

        // 使用线程异步处理，避免阻塞主线程
        CompletableFuture.runAsync(() -> {
            try {
                if (this.state != AgentState.IDLE) {
                    sendStreamMessage(emitter, "error", "无法从状态运行代理: " + this.state);
                    emitter.complete();
                    return;
                }
                if (StringUtil.isBlank(userPrompt)) {
                    sendStreamMessage(emitter, "error", "不能使用空提示词运行代理");
                    emitter.complete();
                    return;
                }

                // 更改状态
                state = AgentState.RUNNING;
                // 记录消息上下文
                messageList.add(new UserMessage(userPrompt));

                // 发送开始思考的状态
                sendStreamMessage(emitter, "thinking", "正在思考中...");

                // 发送任务开始消息（用于Manus面板）
                String[] defaultTasks = {
                        "分析用户请求",
                        "搜索相关信息",
                        "执行工具操作",
                        "生成最终回复"
                };
                sendTaskStart(emitter, defaultTasks);

                try {
                    StringBuilder finalContent = new StringBuilder();

                    for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                        int stepNumber = i + 1;
                        currentStep = stepNumber;
                        log.info("Executing step " + stepNumber + "/" + maxSteps);

                        // 发送任务进度和状态
                        int taskIndex = Math.min(stepNumber - 1, defaultTasks.length - 1);
                        sendTaskProgress(emitter, taskIndex, defaultTasks[taskIndex]);
                        sendStreamMessage(emitter, "status", getStepStatusMessage(stepNumber));

                        // 单步执行
                        String stepResult = step();

                        // 解析并提取有用内容
                        String extractedContent = extractAIContent(stepResult);
                        if (extractedContent != null && !extractedContent.isEmpty()) {
                            finalContent.append(extractedContent);
                            // 发送实际内容
                            sendStreamMessage(emitter, "content", extractedContent);
                        }
                    }

                    // 检查是否超出步骤限制
                    if (currentStep >= maxSteps) {
                        state = AgentState.FINISHED;
                    }

                    // 将最终内容作为 AssistantMessage 添加到 messageList，用于持久化
                    // 这确保了发送给前端的内容也能被正确保存
                    if (finalContent.length() > 0) {
                        messageList.add(new AssistantMessage(finalContent.toString()));
                        log.info("已将 AI 最终回复添加到 messageList 用于持久化: {} 字符", finalContent.length());
                    }

                    // 发送完成标记
                    sendStreamMessage(emitter, "done", "");
                    // 正常完成
                    emitter.complete();
                } catch (Exception e) {
                    state = AgentState.ERROR;
                    log.error("执行智能体失败", e);
                    try {
                        sendStreamMessage(emitter, "error", "执行错误: " + e.getMessage());
                        emitter.complete();
                    } catch (Exception ex) {
                        emitter.completeWithError(ex);
                    }
                } finally {
                    // 清理资源
                    this.cleanup();
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        // 设置超时和完成回调
        emitter.onTimeout(() -> {
            this.state = AgentState.ERROR;
            this.cleanup();
            log.warn("SSE connection timed out");
        });

        emitter.onCompletion(() -> {
            if (this.state == AgentState.RUNNING) {
                this.state = AgentState.FINISHED;
            }
            this.cleanup();
            log.info("SSE connection completed");
        });

        return emitter;
    }

    /**
     * 发送流式消息（JSON格式）
     */
    protected void sendStreamMessage(SseEmitter emitter, String type, String content) {
        try {
            // 使用简单的JSON格式: {"type":"xxx","content":"xxx"}
            String json = String.format("{\"type\":\"%s\",\"content\":\"%s\"}",
                    type,
                    escapeJson(content));
            emitter.send(json);
        } catch (Exception e) {
            log.warn("发送流式消息失败: {}", e.getMessage());
        }
    }

    /**
     * 发送带数据的流式消息（JSON格式，用于Manus面板）
     */
    protected void sendStreamMessageWithData(SseEmitter emitter, String type, String content, String dataJson) {
        try {
            String json;
            if (dataJson != null && !dataJson.isEmpty()) {
                json = String.format("{\"type\":\"%s\",\"content\":\"%s\",\"data\":%s}",
                        type,
                        escapeJson(content),
                        dataJson);
            } else {
                json = String.format("{\"type\":\"%s\",\"content\":\"%s\"}",
                        type,
                        escapeJson(content));
            }
            emitter.send(json);
        } catch (Exception e) {
            log.warn("发送流式消息失败: {}", e.getMessage());
        }
    }

    /**
     * 发送任务开始消息（用于自动打开Manus面板）
     */
    protected void sendTaskStart(SseEmitter emitter, String[] taskNames) {
        StringBuilder tasksJson = new StringBuilder("[");
        for (int i = 0; i < taskNames.length; i++) {
            if (i > 0)
                tasksJson.append(",");
            tasksJson.append("\"").append(escapeJson(taskNames[i])).append("\"");
        }
        tasksJson.append("]");
        sendStreamMessageWithData(emitter, "task_start", "任务开始",
                String.format("{\"tasks\":%s}", tasksJson.toString()));
    }

    /**
     * 发送任务进度更新
     */
    protected void sendTaskProgress(SseEmitter emitter, int stepIndex, String stepName) {
        sendStreamMessageWithData(emitter, "task_progress", stepName,
                String.format("{\"step\":%d,\"name\":\"%s\"}", stepIndex, escapeJson(stepName)));
    }

    /**
     * 发送终端输出
     */
    protected void sendTerminalOutput(SseEmitter emitter, String command, String output) {
        sendStreamMessageWithData(emitter, "terminal", command,
                String.format("{\"command\":\"%s\",\"output\":\"%s\"}",
                        escapeJson(command),
                        escapeJson(output)));
    }

    /**
     * 发送文件创建通知
     */
    protected void sendFileCreated(SseEmitter emitter, String fileType, String fileName, String filePath,
            String fileUrl) {
        sendStreamMessageWithData(emitter, "file_created", fileName,
                String.format("{\"type\":\"%s\",\"name\":\"%s\",\"path\":\"%s\",\"url\":\"%s\"}",
                        escapeJson(fileType),
                        escapeJson(fileName),
                        escapeJson(filePath),
                        escapeJson(fileUrl != null ? fileUrl : "")));
    }

    /**
     * 转义JSON特殊字符
     */
    private String escapeJson(String text) {
        if (text == null)
            return "";
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 获取步骤状态消息
     */
    private String getStepStatusMessage(int stepNumber) {
        String[] statusMessages = {
                "正在分析你的问题...",
                "正在搜索相关信息...",
                "正在整理分析结果...",
                "正在生成建议...",
                "正在优化回复内容..."
        };
        int index = (stepNumber - 1) % statusMessages.length;
        return statusMessages[index];
    }

    /**
     * 从步骤结果中提取AI内容（过滤掉工具调用的技术信息）
     */
    private String extractAIContent(String stepResult) {
        if (stepResult == null || stepResult.isEmpty()) {
            return null;
        }

        // 过滤掉工具调用的技术信息
        if (stepResult.startsWith("Step ")) {
            // 尝试提取Step后面的内容
            int colonIndex = stepResult.indexOf(": ");
            if (colonIndex != -1) {
                stepResult = stepResult.substring(colonIndex + 2);
            }
        }

        // 过滤掉工具执行的技术信息
        if (stepResult.contains("工具 ") && stepResult.contains("完成了它的任务")) {
            // 尝试提取结果部分
            int resultIndex = stepResult.indexOf("结果: ");
            if (resultIndex != -1) {
                String result = stepResult.substring(resultIndex + 4);
                // 如果结果是JSON或技术数据，跳过
                if (result.trim().startsWith("{") || result.trim().startsWith("[")) {
                    return null;
                }
                return result;
            }
            return null;
        }

        // 过滤掉其他技术性输出
        if (stepResult.equals("思考完成 - 无需行动") ||
                stepResult.equals("没有工具调用") ||
                stepResult.startsWith("未检测到工具调用")) {
            return null;
        }

        return stepResult;
    }

}
