package org.example.springai_learn.app;

import java.util.*;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.ChatMemory.DatabaseChatMemory;
import org.example.springai_learn.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class LoveApp {

    private final ChatClient chatClient;

    @Resource
    private ToolCallback[] allTools;

    // 使用@Autowired(required = false)让MCP注入变为可选的
    @Autowired(required = false)
    private ToolCallbackProvider toolCallbackProvider;

    private static final String SYSTEM_PROMPT = "你是Luna，一个像闺蜜一样贴心的恋爱小助手。" +
            "你说话自然、温暖，像好朋友聊天一样，不用生硬的模板或标签。" +
            "\n\n" +
            "【你的风格】\n" +
            "- 像闺蜜一样说话：用\"宝\"\"亲爱的\"\"姐妹\"等亲切称呼，偶尔用点表情包语气（比如\"呜呜\"\"哈哈哈\"\"绝了\"）\n" +
            "- 先给答案，再展开说：用户问什么，先直接回应，然后再细细分析\n" +
            "- 不用任何分段标签：不要[情绪确认][问题分析]这种机器味的东西\n" +
            "- 口语化表达：短句为主，像微信聊天那样自然\n" +
            "\n" +
            "【你能帮什么】\n" +
            "- 分析感情里的纠结：他到底喜不喜欢我？这段关系还要不要继续？\n" +
            "- 解读对方行为：已读不回、忽冷忽热、暧昧不清...\n" +
            "- 约会和聊天技巧：怎么开场、怎么撩、怎么化解尴尬\n" +
            "- 走出失恋：怎么放下、怎么疗伤、怎么重新爱自己\n" +
            "- 经营长期关系：沟通方式、矛盾处理、保持新鲜感\n" +
            "\n" +
            "【聊天原则】\n" +
            "1. 先共情，再给建议。比如：\"我懂这种感觉...\"\"太正常了宝...\"\n" +
            "2. 用开放式提问了解细节，而不是直接下结论\n" +
            "3. 建议用\"你可以试试...\"而不是\"你应该...\"，尊重对方的选择\n" +
            "4. 如果发现危险信号（比如对方有暴力倾向、PUA迹象），要明确提醒并提供帮助资源\n" +
            "5. 不聊政治、宗教、政策这些敏感话题，如果用户提到，温和地转回感情话题\n" +
            "6. 不过度分享\"你自己\"的故事，焦点始终在用户身上\n" +
            "\n" +
            "【回复示例】\n" +
            "用户：\"我喜欢一个男生，但他总是忽冷忽热的，怎么办？\"\n" +
            "你：\"呜呜这种感觉我太懂了宝！忽冷忽热真的让人抓狂..." +
            "先帮你分析一下，这种情况一般有几个可能：要么他在养鱼，要么他自己也没想清楚，要么就是你俩的节奏没对上。" +
            "你可以试试：下次他热情的时候，你也热情回应；他冷淡的时候，你就忙自己的，别追着问。看看他会不会主动来找你。" +
            "记住啊，你的感受最重要，别让自己一直处在患得患失里~\"";

    public LoveApp(@Qualifier("dashscopeChatModel") ChatModel dashscopeChatModel,
                   DatabaseChatMemory databaseChatMemory) {
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(databaseChatMemory)
                )
                .build();
    }


    /**
     * AI基础对话（支持多轮对话记忆）
     *
     * @param message
     * @param chatId
     * @return
     */

    public String doChat(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId) // 哪个id的上下文
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10)) // 多少上下文
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    record LoveReport(String title, List<String> suggestions) {
    }


    /***
     * 对话格式化输出
     * @param message
     * @param chatId
     * @return
     */
    public LoveReport doChatWithReport(String message, String chatId) {
        LoveReport loveReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .entity(LoveReport.class);
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }


    /**
     * 工具类
     *
     * @param message
     * @param chatId
     * @return
     */

    public String doChatWithTools(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .tools(allTools)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }



    public String doChatWithMcp(String message, String chatId) {
        // 检查MCP服务是否可用
        if (toolCallbackProvider == null) {
            log.warn("MCP服务不可用，降级为普通聊天模式");
            return doChat(message, chatId);
        }
        
        try {
            ChatResponse response = chatClient
                    .prompt()
                    .user(message)
                    .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                            .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                    // 开启日志，便于观察效果
                    .advisors(new MyLoggerAdvisor())
                    .tools(toolCallbackProvider)
                    .call()
                    .chatResponse();
            String content = response.getResult().getOutput().getText();
            log.info("content: {}", content);
            return content;
        } catch (Exception e) {
            log.error("MCP聊天失败，降级为普通聊天: {}", e.getMessage());
            return doChat(message, chatId);
        }
    }

    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .stream()
                .content();
    }

    /**
     * 带 RAG 知识和 Intake 分析上下文的对话。
     * 将 RAG 知识和 intake 分析注入系统提示词，用户原始消息作为 user 消息保存至记忆，
     * 避免把内部富化 prompt 写入对话历史。
     *
     * @param originalMessage 用户原始输入，用于记忆存储
     * @param systemContext   额外注入系统提示词的上下文（如 RAG 知识 + intake 分析）
     * @param chatId          会话 ID
     */
    public String doChatWithRAGContext(String originalMessage, String systemContext, String chatId) {
        String enrichedSystem = SYSTEM_PROMPT
                + (systemContext == null || systemContext.isBlank() ? "" : "\n\n" + systemContext);
        ChatResponse response = chatClient
                .prompt()
                .system(enrichedSystem)
                .user(originalMessage)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("doChatWithRAGContext content length={}", content.length());
        return content;
    }

    /**
     * 流式版本： 带 RAG 知识和 Intake 分析上下文的对话，逐 token 发送。
 *
     * @param originalMessage 用户原始输入， 用于记忆存储
     * @param systemContext   额外注入系统提示词的上下文
     * @param chatId          会话 ID
     */
    public Flux<String> doChatWithRAGContextStream(String originalMessage, String systemContext, String chatId) {
        String enrichedSystem = SYSTEM_PROMPT
                + (systemContext == null || systemContext.isBlank() ? "" : "\n\n" + systemContext);
        return chatClient
                .prompt()
                .system(enrichedSystem)
                .user(originalMessage)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .stream()
                .content();
    }

}
