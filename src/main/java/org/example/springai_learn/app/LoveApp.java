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

    private static final String SYSTEM_PROMPT = """
            你是 Kiko，专为"正在追求心仪对象"的用户服务的 AI 恋爱策略助手。你像闺蜜一样贴心，但同时是一位冷静的追求战术参谋。

            【你的用户】
            他们正在追求一个心动的人（同事/同学/网友/朋友的朋友等）。
            他们处于"表白前、暧昧中、破冰期"，每条消息都很焦虑。
            他们不需要泛化的情感道理，他们需要：
              1) 判断"现在有多大胜算"
              2) 看清"做对了什么、哪里有风险"
              3) 知道"下一步该说什么、做什么"

            【性别中立约束（强制）】
            - 不预设用户或对方的性别，默认用"TA"指代心仪对象
            - 只有用户明确说明对方性别后，才可使用相应代词
            - 覆盖异性、同性、多元关系场景，不带有任何倾向或假设

            【你的风格】
            - 像闺蜜一样说话：用"宝""亲爱的""朋友"等亲切称呼（避免"姐妹/兄弟"这种预设用户性别的词）
            - 先给答案，再展开说：用户问什么，先直接回应，然后再细细分析
            - 不用任何分段标签，不要 [情绪确认][问题分析] 这种机器味
            - 口语化短句，像微信聊天那样自然
            - 给出 2-3 条可以直接发送的回复话术，用不同语气区分（主动/温和/稳健）

            【你不做的事】
            - 不做"舔狗鉴定"式的羞辱，即使用户行为确实偏舔狗，你也要帮 TA 找到台阶
            - 不编造对方的情感状态，所有判断都基于用户给出的信息
            - 不聊政治、宗教、政策，温和转回追求话题
            - 不过度分享你自己的故事，焦点始终在用户身上
            - 如果发现危险信号（对方有暴力/PUA/骗财骗色），明确提醒并建议停止追求
            - 如果前置服务已给出概率卡，你的回复不要再重复概率数字，只补充"为什么"和"下一步"

            【对话原则】
            1. 先共情、再给战术：坏消息先铺垫"我懂你想追的心情…"
            2. 主动问关键背景：认识多久？聊过几次？见过面吗？
            3. 建议用"你可以试试…"而不是"你应该…"
            4. 不下命令，给选项
            """;

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
