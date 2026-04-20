package org.example.springai_learn.ai.orchestrator;

import org.example.springai_learn.ChatMemory.DatabaseChatMemory;
import org.example.springai_learn.ai.config.RewriteProperties;
import org.example.springai_learn.ai.context.BrainDecision;
import org.example.springai_learn.ai.context.ChatInputContext;
import org.example.springai_learn.ai.context.ChatMode;
import org.example.springai_learn.ai.context.OcrExtractionResult;
import org.example.springai_learn.ai.context.ToolsAgentResult;
import org.example.springai_learn.ai.service.BrainAgentService;
import org.example.springai_learn.ai.service.ChatRunService;
import org.example.springai_learn.ai.service.MultimodalIntakeService;
import org.example.springai_learn.ai.service.OcrAgentService;
import org.example.springai_learn.ai.service.ProbabilityKeywordDetector;
import org.example.springai_learn.ai.service.RagKnowledgeService;
import org.example.springai_learn.ai.service.SseEventHelper;
import org.example.springai_learn.ai.service.ToolHintDetector;
import org.example.springai_learn.ai.service.ToolsAgentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CoachChatOrchestratorTest {

    private static final int ASYNC_TIMEOUT_MS = 3000;

    private MultimodalIntakeService intakeService;
        private OcrAgentService ocrAgentService;
        private ProbabilityKeywordDetector probabilityKeywordDetector;
        private ToolHintDetector toolHintDetector;
        private RewriteProperties rewriteProperties;
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
        ocrAgentService = mock(OcrAgentService.class);
        probabilityKeywordDetector = mock(ProbabilityKeywordDetector.class);
        toolHintDetector = mock(ToolHintDetector.class);
        rewriteProperties = new RewriteProperties(false, new RewriteProperties.RateLimit(5, 20));
        ragKnowledgeService = mock(RagKnowledgeService.class);
        brainAgentService = mock(BrainAgentService.class);
        toolsAgentService = mock(ToolsAgentService.class);
        sseEventHelper = mock(SseEventHelper.class);
        databaseChatMemory = mock(DatabaseChatMemory.class);
        chatRunService = mock(ChatRunService.class);

        orchestrator = new CoachChatOrchestrator(
                intakeService,
                ocrAgentService,
                probabilityKeywordDetector,
                toolHintDetector,
                rewriteProperties,
                ragKnowledgeService,
                brainAgentService,
                toolsAgentService,
                sseEventHelper,
                databaseChatMemory,
                chatRunService
        );
    }

    // --- 直接回答路径 ---

    @Test
    void stream_directAnswer_shouldCallBrainAndReturnDirectly() {
        ChatInputContext context = new ChatInputContext(
                "user-1", "chat-1", ChatMode.COACH, "他在跟我玩欲擒故纵吗", null);

        when(toolHintDetector.detect(context.userMessage())).thenReturn(false);
        when(brainAgentService.streamDirectAnswer(context, ""))
                .thenReturn(Flux.just("你的判断有一定道理，但不要过早下结论。"));

        orchestrator.stream(context, "run-1");

        verify(ragKnowledgeService, never()).retrieveKnowledge(any());
        verify(brainAgentService, never()).decide(any(), any(), any());
        verify(brainAgentService, timeout(ASYNC_TIMEOUT_MS)).streamDirectAnswer(context, "");
        verify(sseEventHelper, timeout(ASYNC_TIMEOUT_MS))
                .send(any(), eq("content"), eq("你的判断有一定道理，但不要过早下结论。"));
        verify(toolsAgentService, never()).activate(any(), any(), any());
    }

    @Test
    void stream_directAnswer_withImage_shouldPersistImageUrl() {
        ChatInputContext context = new ChatInputContext(
                "user-1", "chat-2", ChatMode.COACH, "帮我分析这段对话",
                "/api/images/user-1/chat/shot.png");

        when(ocrAgentService.extract(context.imageUrl(), context.userId(), context.userMessage()))
                .thenReturn(OcrExtractionResult.of("截图文字内容", "截图摘要"));
        when(toolHintDetector.detect(context.userMessage())).thenReturn(false);
        when(probabilityKeywordDetector.detect(context.userMessage())).thenReturn(false);
        when(ragKnowledgeService.retrieveKnowledge("截图摘要 / 帮我分析这段对话")).thenReturn("");
        when(brainAgentService.decide(any(), any(), any()))
                .thenReturn(BrainDecision.directAnswer("这是我的分析结果。", "", "我来分析。"));
        when(brainAgentService.streamDirectAnswer(context, ""))
                .thenReturn(Flux.just("这是我的分析结果。"));

        orchestrator.stream(context, "run-2");

        verify(brainAgentService, timeout(ASYNC_TIMEOUT_MS)).decide(any(), any(), any());
        verify(brainAgentService, timeout(ASYNC_TIMEOUT_MS)).streamDirectAnswer(context, "");

        verify(databaseChatMemory, timeout(ASYNC_TIMEOUT_MS))
                .setImageUrlOnLatestUserMessage(
                        eq("chat-2"), eq("/api/images/user-1/chat/shot.png"));
    }

    @Test
    void stream_directAnswer_shouldSaveConversationWithCompositeKey() {
        ChatInputContext context = new ChatInputContext(
                "user-1", "chat-3", ChatMode.COACH, "我该怎么约他出来", null);

        when(toolHintDetector.detect(context.userMessage())).thenReturn(false);
        when(brainAgentService.streamDirectAnswer(context, ""))
                .thenReturn(Flux.just("以轻松的方式提出约会邀请。"));

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

        BrainDecision toolDecision = BrainDecision.useTools(
                "搜索异地恋见面安排攻略并整理成计划",
                "用户想要异地恋见面计划",
                "宝，我帮你查点资料整理一下，稍等哈~");

        when(toolHintDetector.detect(context.userMessage())).thenReturn(true);
        when(probabilityKeywordDetector.detect(context.userMessage())).thenReturn(false);
        when(ragKnowledgeService.retrieveKnowledge(any())).thenReturn("");
        when(brainAgentService.decide(any(), any(), any())).thenReturn(toolDecision);
        ToolsAgentResult toolResult = new ToolsAgentResult("搜索结果：异地恋见面建议包括...", List.of());
        when(toolsAgentService.activate(eq(toolDecision), anyString(), any()))
                .thenReturn(toolResult);
        when(brainAgentService.streamSynthesize(eq(toolDecision), eq("搜索结果：异地恋见面建议包括..."), eq(List.of())))
                .thenReturn(Flux.just("宝，我帮你整理好了异地恋见面攻略！"));

        orchestrator.stream(context, "run-4");

        verify(toolsAgentService, timeout(ASYNC_TIMEOUT_MS))
                .activate(eq(toolDecision), anyString(), any());
        verify(brainAgentService, timeout(ASYNC_TIMEOUT_MS))
                .streamSynthesize(eq(toolDecision), eq("搜索结果：异地恋见面建议包括..."), eq(List.of()));
        verify(sseEventHelper, timeout(ASYNC_TIMEOUT_MS))
                .send(any(), eq("content"), eq("宝，我帮你整理好了异地恋见面攻略！"));
    }

    // --- 异常处理 ---

    @Test
    void stream_shouldSendErrorEvent_whenOcrThrows() {
        ChatInputContext context = new ChatInputContext(
                "user-1", "chat-6", ChatMode.COACH, "测试", "/api/images/user-1/chat/error.png");

        when(ocrAgentService.extract(context.imageUrl(), context.userId(), context.userMessage()))
                .thenThrow(new RuntimeException("OCR 服务不可用"));
        when(toolHintDetector.detect(context.userMessage())).thenReturn(true);

        orchestrator.stream(context, "run-5");

        verify(sseEventHelper, timeout(ASYNC_TIMEOUT_MS))
                .send(any(), eq("error"), contains("处理失败"));
        verify(ragKnowledgeService, never()).retrieveKnowledge(any());
    }

    @Test
    void stream_withImage_whenOcrFailed_shouldStillContinue() {
        ChatInputContext context = new ChatInputContext(
                "user-1", "chat-7", ChatMode.COACH, "我该怎么回", "/api/images/user-1/chat/fail.png");

        when(ocrAgentService.extract(context.imageUrl(), context.userId(), context.userMessage()))
                .thenReturn(OcrExtractionResult.failed("OCR 模型超时"));
        when(toolHintDetector.detect(context.userMessage())).thenReturn(false);
        when(probabilityKeywordDetector.detect(context.userMessage())).thenReturn(false);
        when(ragKnowledgeService.retrieveKnowledge(any())).thenReturn("");
        when(brainAgentService.decide(any(), any(), any()))
                .thenReturn(BrainDecision.directAnswer("可以先冷静再回应。", "", "先稳住"));
        when(brainAgentService.streamDirectAnswer(context, ""))
                .thenReturn(Flux.just("可以先冷静再回应。"));

        orchestrator.stream(context, "run-7");

        verify(ragKnowledgeService, timeout(ASYNC_TIMEOUT_MS)).retrieveKnowledge(contains("我该怎么回"));
        verify(brainAgentService, timeout(ASYNC_TIMEOUT_MS)).streamDirectAnswer(context, "");
        verify(sseEventHelper, timeout(ASYNC_TIMEOUT_MS)).send(any(), eq("content"), eq("可以先冷静再回应。"));
    }
}
