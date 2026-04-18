package org.example.springai_learn.ai.orchestrator;

import org.example.springai_learn.ChatMemory.DatabaseChatMemory;
import org.example.springai_learn.ai.context.BrainDecision;
import org.example.springai_learn.ai.context.ChatInputContext;
import org.example.springai_learn.ai.context.ChatMode;
import org.example.springai_learn.ai.context.IntakeAnalysisResult;
import org.example.springai_learn.ai.context.ToolsAgentResult;
import org.example.springai_learn.ai.service.BrainAgentService;
import org.example.springai_learn.ai.service.ChatRunService;
import org.example.springai_learn.ai.service.MultimodalIntakeService;
import org.example.springai_learn.ai.service.RagKnowledgeService;
import org.example.springai_learn.ai.service.SseEventHelper;
import org.example.springai_learn.ai.service.ToolsAgentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CoachChatOrchestratorTest {

    private static final int ASYNC_TIMEOUT_MS = 3000;

    private MultimodalIntakeService intakeService;
    private RagKnowledgeService ragKnowledgeService;
    private BrainAgentService brainAgentService;
    private ToolsAgentService toolsAgentService;
    private SseEventHelper sseEventHelper;
    private DatabaseChatMemory databaseChatMemory;
    private ChatRunService chatRunService;
    private CoachChatOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        intakeService = mock(MultimodalIntakeService.class);
        ragKnowledgeService = mock(RagKnowledgeService.class);
        brainAgentService = mock(BrainAgentService.class);
        toolsAgentService = mock(ToolsAgentService.class);
        sseEventHelper = mock(SseEventHelper.class);
        databaseChatMemory = mock(DatabaseChatMemory.class);
        chatRunService = mock(ChatRunService.class);

        orchestrator = new CoachChatOrchestrator(
                intakeService, ragKnowledgeService, brainAgentService,
                toolsAgentService, sseEventHelper, databaseChatMemory, chatRunService);
    }

    // --- 直接回答路径 ---

    @Test
    void stream_directAnswer_shouldCallBrainAndReturnDirectly() {
        ChatInputContext context = new ChatInputContext(
                "user-1", "chat-1", ChatMode.COACH, "他在跟我玩欲擒故纵吗", null);

        IntakeAnalysisResult analysis = new IntakeAnalysisResult(
                false, "", "对方态度模糊",
                "请分析对方的态度并给出应对策略",
                List.of(), "判断对方意图", false, false);

        when(intakeService.analyze(context)).thenReturn(analysis);
        when(ragKnowledgeService.retrieveKnowledge("请分析对方的态度并给出应对策略"))
                .thenReturn("欲擒故纵的常见信号包括...");
        when(brainAgentService.decide(eq(context), eq(analysis), eq("欲擒故纵的常见信号包括...")))
                .thenReturn(BrainDecision.directAnswer(
                        "你的判断有一定道理，但不要过早下结论。",
                        "", "我来直接分析这个问题。"));

        orchestrator.stream(context, "run-1");

        verify(ragKnowledgeService, timeout(ASYNC_TIMEOUT_MS))
                .retrieveKnowledge("请分析对方的态度并给出应对策略");
        verify(brainAgentService, timeout(ASYNC_TIMEOUT_MS))
                .decide(eq(context), eq(analysis), eq("欲擒故纵的常见信号包括..."));
        verify(sseEventHelper, timeout(ASYNC_TIMEOUT_MS))
                .send(any(), eq("content"), eq("你的判断有一定道理，但不要过早下结论。"));
        verify(toolsAgentService, never()).activate(any(), any(), any());
    }

    @Test
    void stream_directAnswer_withImage_shouldPersistImageUrl() {
        ChatInputContext context = new ChatInputContext(
                "user-1", "chat-2", ChatMode.COACH, "帮我分析这段对话",
                "/api/images/user-1/chat/shot.png");

        IntakeAnalysisResult analysis = new IntakeAnalysisResult(
                true, "截图文字内容", "截图摘要", "分析截图中的对话",
                List.of(), "获取建议", false, false);

        when(intakeService.analyze(context)).thenReturn(analysis);
        when(ragKnowledgeService.retrieveKnowledge(any())).thenReturn("");
        when(brainAgentService.decide(any(), any(), any()))
                .thenReturn(BrainDecision.directAnswer(
                        "这是我的分析结果。", "", "我来分析。"));

        orchestrator.stream(context, "run-2");

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
                List.of(), "获取约会建议", false, false);

        when(intakeService.analyze(context)).thenReturn(analysis);
        when(ragKnowledgeService.retrieveKnowledge(any())).thenReturn("");
        when(brainAgentService.decide(any(), any(), any()))
                .thenReturn(BrainDecision.directAnswer(
                        "以轻松的方式提出约会邀请。", "", "好的。"));

        orchestrator.stream(context, "run-3");

        ArgumentCaptor<String> conversationIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(databaseChatMemory, timeout(ASYNC_TIMEOUT_MS).times(2))
                .add(conversationIdCaptor.capture(), anyList());

        // 用户消息立即持久化 + assistant 消息在回答生成后持久化，两次调用使用相同的 conversationId
        String cid = conversationIdCaptor.getAllValues().getFirst();
        assertTrue(cid.contains("user-1"), "conversationId 应包含 userId");
        assertTrue(cid.contains("coach"), "conversationId 应包含 chatType");
        assertTrue(cid.contains("chat-3"), "conversationId 应包含 chatId");
    }

    // --- 工具路径 ---

    @Test
    void stream_toolPath_shouldActivateToolsAgentAndSynthesize() {
        ChatInputContext context = new ChatInputContext(
                "user-1", "chat-4", ChatMode.COACH,
                "帮我查一下异地恋见面怎么安排", null);

        IntakeAnalysisResult analysis = new IntakeAnalysisResult(
                false, "", "需要查资料", "异地恋如何维系",
                List.of(), "搜索并整理", true, false);

        BrainDecision toolDecision = BrainDecision.useTools(
                "搜索异地恋见面安排攻略并整理成计划",
                "用户想要异地恋见面计划",
                "宝，我帮你查点资料整理一下，稍等哈~");

        when(intakeService.analyze(context)).thenReturn(analysis);
        when(ragKnowledgeService.retrieveKnowledge(any())).thenReturn("");
        when(brainAgentService.decide(any(), any(), any())).thenReturn(toolDecision);
        ToolsAgentResult toolResult = new ToolsAgentResult("搜索结果：异地恋见面建议包括...", List.of());
        when(toolsAgentService.activate(eq(toolDecision), anyString(), any()))
                .thenReturn(toolResult);
        when(brainAgentService.synthesize(eq(toolDecision), eq("搜索结果：异地恋见面建议包括..."), eq(List.of())))
                .thenReturn("宝，我帮你整理好了异地恋见面攻略！");

        orchestrator.stream(context, "run-4");

        verify(toolsAgentService, timeout(ASYNC_TIMEOUT_MS))
                .activate(eq(toolDecision), anyString(), any());
        verify(brainAgentService, timeout(ASYNC_TIMEOUT_MS))
                .synthesize(eq(toolDecision), eq("搜索结果：异地恋见面建议包括..."), eq(List.of()));
        verify(sseEventHelper, timeout(ASYNC_TIMEOUT_MS))
                .send(any(), eq("content"), eq("宝，我帮你整理好了异地恋见面攻略！"));
    }

    // --- 异常处理 ---

    @Test
    void stream_shouldSendErrorEvent_whenIntakeThrows() {
        ChatInputContext context = new ChatInputContext(
                "user-1", "chat-6", ChatMode.COACH, "测试", null);

        when(intakeService.analyze(context)).thenThrow(new RuntimeException("OCR 服务不可用"));

        orchestrator.stream(context, "run-5");

        verify(sseEventHelper, timeout(ASYNC_TIMEOUT_MS))
                .send(any(), eq("error"), contains("处理失败"));
        verify(ragKnowledgeService, never()).retrieveKnowledge(any());
    }
}
