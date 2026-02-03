package org.example.springai_learn.agent;

import org.example.springai_learn.ChatMemory.FileBasedChatMemory;
import org.example.springai_learn.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class KkomaManus extends ToolCallAgent {

        private static final String COACH_MEMORY_DIR = System.getProperty("user.dir") + "/tmp/chat-coach";

        private final String chatId;
        private final ChatMemory chatMemory;

        public KkomaManus(ToolCallback[] allTools, ChatModel dashscopeChatModel,
                        ToolCallbackProvider mcpToolCallbackProvider, String chatId) {
                super(allTools, mcpToolCallbackProvider);
                this.chatId = chatId;
                this.chatMemory = new FileBasedChatMemory(COACH_MEMORY_DIR);

                this.setName("KkomaManus");
                String SYSTEM_PROMPT = """
                                You are KkomaManus, an autonomous AI agent aimed at solving any task presented by the user.
                                You have various tools at your disposal that you can call upon to efficiently complete complex requests.

                                IMPORTANT RULES:
                                1. You are FULLY AUTONOMOUS - never ask the user for confirmation, clarification, or approval.
                                2. Make all decisions yourself based on reasonable assumptions.
                                3. If information is missing, make sensible default choices and proceed.
                                4. Execute tasks directly using tools - do not just describe what you would do.
                                5. Only output the final results to the user, not intermediate questions.
                                6. Always call at least one tool per step until the task is complete.
                                """;
                this.setSystemPrompt(SYSTEM_PROMPT);
                String NEXT_STEP_PROMPT = """
                                Analyze the current state and immediately take action using the most appropriate tool.
                                Do NOT ask the user any questions - make autonomous decisions.
                                Do NOT explain what you plan to do - just do it by calling tools.
                                If you have completed the task, call the `doTerminate` tool with a summary of results.
                                If there are more steps needed, call the next required tool now.
                                """;
                this.setNextStepPrompt(NEXT_STEP_PROMPT);
                this.setMaxSteps(20);

                // 初始化客户端
                ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                                .defaultAdvisors(new MyLoggerAdvisor())
                                .build();
                this.setChatClient(chatClient);

                // 从 ChatMemory 加载历史消息
                loadChatHistory();
        }

        /**
         * 从 ChatMemory 加载历史对话
         */
        private void loadChatHistory() {
                if (chatId == null || chatId.isEmpty()) {
                        log.info("无 chatId，不加载历史消息");
                        return;
                }
                try {
                        List<Message> history = chatMemory.get(chatId, 20); // 最多加载20条历史
                        if (!history.isEmpty()) {
                                log.info("从 ChatMemory 加载了 {} 条历史消息: chatId={}", history.size(), chatId);
                                // 将历史消息添加到 messageList
                                getMessageList().addAll(history);
                        }
                } catch (Exception e) {
                        log.warn("加载历史消息失败: chatId={}, error={}", chatId, e.getMessage());
                }
        }

        /**
         * 保存当前对话到 ChatMemory
         * 注意：需要过滤掉内部的系统提示（如 nextStepPrompt）
         */
        public void saveToChatMemory() {
                if (chatId == null || chatId.isEmpty()) {
                        log.info("无 chatId，不保存消息");
                        return;
                }
                try {
                        // 获取当前会话的所有消息，过滤保存 User 和 Assistant 消息
                        // 但要排除内部系统提示（如 nextStepPrompt）
                        List<Message> messagesToSave = new ArrayList<>();
                        for (Message msg : getMessageList()) {
                                if (msg instanceof UserMessage userMsg) {
                                        String text = userMsg.getText();
                                        // 过滤掉内部提示（nextStepPrompt）
                                        if (text != null && !text.trim().startsWith("Analyze the current state")) {
                                                messagesToSave.add(msg);
                                        }
                                } else if (msg instanceof AssistantMessage assistantMsg) {
                                        // 过滤掉空的助手消息
                                        String text = assistantMsg.getText();
                                        if (text != null && !text.trim().isEmpty()) {
                                                messagesToSave.add(msg);
                                        }
                                }
                        }
                        if (!messagesToSave.isEmpty()) {
                                // 清除旧的，保存新的完整历史
                                chatMemory.clear(chatId);
                                chatMemory.add(chatId, messagesToSave);
                                log.info("已保存 {} 条消息到 ChatMemory: chatId={}", messagesToSave.size(), chatId);
                        }
                } catch (Exception e) {
                        log.error("保存消息到 ChatMemory 失败: chatId={}, error={}", chatId, e.getMessage(), e);
                }
        }

        public String getChatId() {
                return chatId;
        }
}
