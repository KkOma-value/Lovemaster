package org.example.springai_learn.ai.orchestrator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.ChatMemory.DatabaseChatMemory;
import org.example.springai_learn.agent.KkomaManus;
import org.example.springai_learn.ai.context.ChatInputContext;
import org.example.springai_learn.ai.context.CoachRoutingDecision;
import org.example.springai_learn.ai.context.ConversationIds;
import org.example.springai_learn.ai.context.IntakeAnalysisResult;
import org.example.springai_learn.ai.service.CoachRoutingService;
import org.example.springai_learn.ai.service.MultimodalIntakeService;
import org.example.springai_learn.ai.service.RagKnowledgeService;
import org.example.springai_learn.ai.service.SseEventHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoachChatOrchestrator {

    private final MultimodalIntakeService multimodalIntakeService;
    private final RagKnowledgeService ragKnowledgeService;
    private final CoachRoutingService coachRoutingService;
    private final SseEventHelper sseEventHelper;
    private final ToolCallback[] allTools;
    private final ChatModel dashscopeChatModel;
    private final DatabaseChatMemory databaseChatMemory;

    @Autowired(required = false)
    private ToolCallbackProvider mcpToolCallbackProvider;

    @Value("${app.file-save-dir:${user.dir}/tmp}")
    private String baseDir;

    public SseEmitter stream(ChatInputContext context) {
        SseEmitter emitter = new SseEmitter(300000L);
        emitter.onTimeout(() -> log.warn("CoachChatOrchestrator SSE 超时: chatId={}", context.chatId()));

        Thread.startVirtualThread(() -> {
            try {
                // Step 1: Intake — OCR + structured rewrite
                sseEventHelper.send(emitter, "intake_status",
                        context.hasImage()
                                ? "正在读取聊天截图，并判断这件事要不要进入任务执行..."
                                : "正在整理你的问题，并判断是否需要进入任务执行...");
                IntakeAnalysisResult analysis = multimodalIntakeService.analyze(context);

                if (analysis.hasImage()) {
                    String ocrHint = analysis.ocrText() == null || analysis.ocrText().isBlank()
                            ? "截图识别完成，正在提炼关键上下文。"
                            : "截图识别完成：" + shorten(analysis.ocrText(), 88);
                    sseEventHelper.send(emitter, "ocr_result", ocrHint);
                }

                sseEventHelper.send(emitter, "rewrite_result",
                        "我已经把任务整理成更清晰的问题：" + shorten(analysis.rewrittenQuestion(), 96));

                // Step 2: RAG — retrieve relevant knowledge using rewritten question
                sseEventHelper.send(emitter, "rag_status", "正在查阅恋爱知识库，补充参考资料...");
                String ragKnowledge = ragKnowledgeService.retrieveKnowledge(analysis.rewrittenQuestion());

                // Step 3: Route — decide whether to use tools
                CoachRoutingDecision decision = coachRoutingService.decide(context, analysis, ragKnowledge);
                sseEventHelper.send(emitter, "status", decision.userFacingPrelude());

                if (!decision.shouldUseTools()) {
                    String answer = generateDirectCoachAnswer(decision.directAnswerPrompt());
                    saveCoachConversation(context, answer);
                    if (context.hasImage()) {
                        databaseChatMemory.setImageUrlOnLatestUserMessage(context.chatId(), context.imageUrl());
                    }
                    sseEventHelper.send(emitter, "content", answer);
                    sseEventHelper.done(emitter);
                    emitter.complete();
                    return;
                }

                // Step 4: Tools — delegate to KkomaManus
                sseEventHelper.send(emitter, "tool_call", "我开始进入任务执行，继续帮你搜索、整理并产出结果。");
                String conversationId = ConversationIds.forMode(context.userId(), context.mode(), context.chatId());
                KkomaManus kkomaManus = new KkomaManus(allTools, dashscopeChatModel, mcpToolCallbackProvider,
                        conversationId, databaseChatMemory);
                emitter.onCompletion(() -> {
                    kkomaManus.saveToChatMemory();
                    if (context.hasImage()) {
                        databaseChatMemory.setImageUrlOnLatestUserMessage(context.chatId(), context.imageUrl());
                    }
                });
                emitter.onError(ex -> {
                    kkomaManus.saveToChatMemory();
                    if (context.hasImage()) {
                        databaseChatMemory.setImageUrlOnLatestUserMessage(context.chatId(), context.imageUrl());
                    }
                });
                kkomaManus.runStream(decision.toolTaskPrompt(), emitter);
            } catch (Exception e) {
                log.error("CoachChatOrchestrator 处理失败: {}", e.getMessage(), e);
                sseEventHelper.send(emitter, "error", "处理失败：" + e.getMessage());
                emitter.complete();
            }
        });

        return emitter;
    }

    private String shorten(String text, int limit) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.length() <= limit ? text : text.substring(0, limit) + "...";
    }

    private String generateDirectCoachAnswer(String promptText) {
        String systemPrompt = """
                你是 Lovemaster 的 Coach 模式大脑代理。
                你的工作是先讲清楚局势，再给出可执行建议。
                如果当前不需要工具执行，就直接把结论说透，但不要假装已经进行了搜索或外部调查。
                """;
        ChatResponse response = dashscopeChatModel.call(new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(promptText)
        )));
        return response.getResult().getOutput().getText();
    }

    private void saveCoachConversation(ChatInputContext context, String answer) {
        String compositeId = ConversationIds.forMode(context.userId(), context.mode(), context.chatId());
        databaseChatMemory.add(compositeId, List.of(
                new UserMessage(context.userMessage()),
                new AssistantMessage(answer)
        ));
    }
}
