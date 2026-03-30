package org.example.springai_learn.ai.orchestrator;

import org.example.springai_learn.ChatMemory.DatabaseChatMemory;
import org.example.springai_learn.ai.context.ChatInputContext;
import org.example.springai_learn.ai.context.ChatMode;
import org.example.springai_learn.ai.context.CoachRoutingDecision;
import org.example.springai_learn.ai.context.IntakeAnalysisResult;
import org.example.springai_learn.ai.service.CoachRoutingService;
import org.example.springai_learn.ai.service.MultimodalIntakeService;
import org.example.springai_learn.ai.service.RagKnowledgeService;
import org.example.springai_learn.ai.service.SseEventHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CoachChatOrchestratorTest {

    private static final int ASYNC_TIMEOUT_MS = 3000;

    private MultimodalIntakeService intakeService;
    private RagKnowledgeService ragKnowledgeService;
    private CoachRoutingService routingService;
    private SseEventHelper sseEventHelper;
    private ChatModel brainModel;
    private ChatModel toolsModel;
    private DatabaseChatMemory databaseChatMemory;
    private CoachChatOrchestrator orchestrator;

    @BeforeEach
    void setUp() throws Exception {
        intakeService = mock(MultimodalIntakeService.class);
        ragKnowledgeService = mock(RagKnowledgeService.class);
        routingService = mock(CoachRoutingService.class);
        sseEventHelper = mock(SseEventHelper.class);
        brainModel = mock(ChatModel.class);
        toolsModel = mock(ChatModel.class);
        databaseChatMemory = mock(DatabaseChatMemory.class);

        orchestrator = new CoachChatOrchestrator(
                intakeService, ragKnowledgeService, routingService,
                sseEventHelper, new ToolCallback[]{}, brainModel, toolsModel, databaseChatMemory);

        setField(orchestrator, "baseDir", "/tmp");
    }

    // --- 直接回答路径 ---

    @Test
    void stream_directAnswer_shouldCallRagAndPassKnowledgeToRouting() {
        ChatInputContext context = new ChatInputContext(
                "user-1", "chat-1", ChatMode.COACH, "他在跟我玩欲擒故纵吗", null);

        IntakeAnalysisResult analysis = new IntakeAnalysisResult(
                false, "", "对方态度模糊",
                "请分析对方的态度并给出应对策略",
                List.of(), "判断对方意图", false);

        mockChatModelResponse("你的判断有一定道理，但不要过早下结论。");
        when(intakeService.analyze(context)).thenReturn(analysis);
        when(ragKnowledgeService.retrieveKnowledge("请分析对方的态度并给出应对策略"))
                .thenReturn("欲擒故纵的常见信号包括...");
        when(routingService.decide(eq(context), eq(analysis), eq("欲擒故纵的常见信号包括...")))
                .thenReturn(new CoachRoutingDecision(false, "请分析对方态度。", "",
                        "我来直接分析这个问题。"));

        orchestrator.stream(context);

        verify(ragKnowledgeService, timeout(ASYNC_TIMEOUT_MS))
                .retrieveKnowledge("请分析对方的态度并给出应对策略");
        verify(routingService, timeout(ASYNC_TIMEOUT_MS))
                .decide(eq(context), eq(analysis), eq("欲擒故纵的常见信号包括..."));
        verify(sseEventHelper, timeout(ASYNC_TIMEOUT_MS))
                .send(any(), eq("rag_status"), contains("知识库"));
        verify(sseEventHelper, timeout(ASYNC_TIMEOUT_MS))
                .send(any(), eq("content"), anyString());
    }

    @Test
    void stream_directAnswer_withImage_shouldPersistImageUrl() {
        ChatInputContext context = new ChatInputContext(
                "user-1", "chat-2", ChatMode.COACH, "帮我分析这段对话",
                "/api/images/user-1/chat/shot.png");

        IntakeAnalysisResult analysis = new IntakeAnalysisResult(
                true, "截图文字内容", "截图摘要", "分析截图中的对话",
                List.of(), "获取建议", false);

        mockChatModelResponse("这是我的分析结果。");
        when(intakeService.analyze(context)).thenReturn(analysis);
        when(ragKnowledgeService.retrieveKnowledge(any())).thenReturn("");
        when(routingService.decide(any(), any(), any()))
                .thenReturn(new CoachRoutingDecision(false, "请分析。", "", "我来分析。"));

        orchestrator.stream(context);

        verify(databaseChatMemory, timeout(ASYNC_TIMEOUT_MS))
                .setImageUrlOnLatestUserMessage(
                        eq("chat-2"), eq("/api/images/user-1/chat/shot.png"));
    }

    @Test
    void stream_directAnswer_shouldSaveConversationWithCompositeKey() {
        ChatInputContext context = new ChatInputContext(
                "user-1", "chat-3", ChatMode.COACH, "我该怎么约他出来", null);

        IntakeAnalysisResult analysis = new IntakeAnalysisResult(
                false, "", "想约对方", "如何主动约对方出来",
                List.of(), "获取约会建议", false);

        mockChatModelResponse("以轻松的方式提出约会邀请。");
        when(intakeService.analyze(context)).thenReturn(analysis);
        when(ragKnowledgeService.retrieveKnowledge(any())).thenReturn("");
        when(routingService.decide(any(), any(), any()))
                .thenReturn(new CoachRoutingDecision(false, "请给建议。", "", "好的。"));

        orchestrator.stream(context);

        ArgumentCaptor<String> conversationIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(databaseChatMemory, timeout(ASYNC_TIMEOUT_MS))
                .add(conversationIdCaptor.capture(), anyList());

        String cid = conversationIdCaptor.getValue();
        assertTrue(cid.contains("user-1"), "conversationId 应包含 userId");
        assertTrue(cid.contains("coach"), "conversationId 应包含 chatType");
        assertTrue(cid.contains("chat-3"), "conversationId 应包含 chatId");
    }

    // --- RAG 知识传递校验 ---

    @Test
    void stream_shouldPassRagKnowledgeToRoutingDecision() {
        ChatInputContext context = new ChatInputContext(
                "user-1", "chat-5", ChatMode.COACH, "帮我查异地恋维系技巧", null);

        IntakeAnalysisResult analysis = new IntakeAnalysisResult(
                false, "", "需要查资料", "异地恋如何维系",
                List.of(), "搜索并整理", true);

        String ragKnowledge = "异地恋维系技巧：定期视频、共同计划...";
        mockChatModelResponse("保持沟通节奏。");
        when(intakeService.analyze(context)).thenReturn(analysis);
        when(ragKnowledgeService.retrieveKnowledge("异地恋如何维系")).thenReturn(ragKnowledge);
        when(routingService.decide(eq(context), eq(analysis), eq(ragKnowledge)))
                .thenReturn(new CoachRoutingDecision(false, "请直接分析。", "", "我直接分析。"));

        orchestrator.stream(context);

        verify(routingService, timeout(ASYNC_TIMEOUT_MS))
                .decide(eq(context), eq(analysis), eq(ragKnowledge));
    }

    // --- 异常处理 ---

    @Test
    void stream_shouldSendErrorEvent_whenIntakeThrows() {
        ChatInputContext context = new ChatInputContext(
                "user-1", "chat-6", ChatMode.COACH, "测试", null);

        when(intakeService.analyze(context)).thenThrow(new RuntimeException("OCR 服务不可用"));

        orchestrator.stream(context);

        verify(sseEventHelper, timeout(ASYNC_TIMEOUT_MS))
                .send(any(), eq("error"), contains("处理失败"));
        verify(ragKnowledgeService, never()).retrieveKnowledge(any());
    }

    // --- 工具方法 ---

    private void mockChatModelResponse(String text) {
        ChatResponse chatResponse = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        when(generation.getOutput()).thenReturn(new AssistantMessage(text));
        when(chatResponse.getResult()).thenReturn(generation);
        doReturn(chatResponse).when(brainModel).call(any(Prompt.class));
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) {
                return findField(clazz.getSuperclass(), fieldName);
            }
            throw e;
        }
    }
}
