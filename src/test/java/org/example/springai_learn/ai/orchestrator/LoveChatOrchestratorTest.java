package org.example.springai_learn.ai.orchestrator;

import org.example.springai_learn.ChatMemory.DatabaseChatMemory;
import org.example.springai_learn.ai.config.RewriteProperties;
import org.example.springai_learn.ai.context.ChatInputContext;
import org.example.springai_learn.ai.context.ChatMode;
import org.example.springai_learn.ai.context.OcrExtractionResult;
import org.example.springai_learn.ai.service.ChatRunService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.springai_learn.ai.service.MultimodalIntakeService;
import org.example.springai_learn.ai.service.OcrAgentService;
import org.example.springai_learn.ai.service.ProbabilityAnalysisService;
import org.example.springai_learn.ai.service.ProbabilityKeywordDetector;
import org.example.springai_learn.ai.service.RagKnowledgeService;
import org.example.springai_learn.ai.service.SseEventHelper;
import org.example.springai_learn.ai.service.ToolHintDetector;
import org.example.springai_learn.app.LoveApp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LoveChatOrchestratorTest {

    private static final int ASYNC_TIMEOUT_MS = 3000;

    private MultimodalIntakeService intakeService;
        private OcrAgentService ocrAgentService;
        private ProbabilityKeywordDetector probabilityKeywordDetector;
        private ToolHintDetector toolHintDetector;
        private RewriteProperties rewriteProperties;
    private ProbabilityAnalysisService probabilityAnalysisService;
    private RagKnowledgeService ragKnowledgeService;
    private SseEventHelper sseEventHelper;
    private LoveApp loveApp;
    private DatabaseChatMemory databaseChatMemory;
    private ChatRunService chatRunService;
    private ObjectMapper objectMapper;
    private LoveChatOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        intakeService = mock(MultimodalIntakeService.class);
        ocrAgentService = mock(OcrAgentService.class);
        probabilityKeywordDetector = mock(ProbabilityKeywordDetector.class);
        toolHintDetector = mock(ToolHintDetector.class);
        rewriteProperties = new RewriteProperties(false, new RewriteProperties.RateLimit(5, 20));
        probabilityAnalysisService = mock(ProbabilityAnalysisService.class);
        ragKnowledgeService = mock(RagKnowledgeService.class);
        sseEventHelper = mock(SseEventHelper.class);
        loveApp = mock(LoveApp.class);
        databaseChatMemory = mock(DatabaseChatMemory.class);
        chatRunService = mock(ChatRunService.class);
        objectMapper = new ObjectMapper();
        orchestrator = new LoveChatOrchestrator(
                intakeService,
                ocrAgentService,
                probabilityKeywordDetector,
                toolHintDetector,
                rewriteProperties,
                probabilityAnalysisService,
                ragKnowledgeService,
                sseEventHelper,
                loveApp,
                databaseChatMemory,
                chatRunService,
                objectMapper
        );
    }

    // --- 文本输入流程 ---

    @Test
    void stream_textOnly_shouldCallRagAndReturnAnswer() {
        ChatInputContext context = new ChatInputContext(
                "user-1", "chat-1", ChatMode.LOVE, "他突然不回消息了怎么办", null);

        when(toolHintDetector.detect(context.userMessage())).thenReturn(false);
        when(probabilityKeywordDetector.detect(context.userMessage())).thenReturn(false);
        when(ragKnowledgeService.retrieveKnowledge("他突然不回消息了怎么办"))
                .thenReturn("恋爱中保持冷静是关键。");
        when(loveApp.doChatWithRAGContextStream(any(), any(), eq("user-1:loveapp:chat-1")))
                .thenReturn(Flux.just("建议先不要催他，观察 24 小时后再发一条轻松的消息。"));

        orchestrator.stream(context, "run-1");

        verify(ragKnowledgeService, timeout(ASYNC_TIMEOUT_MS))
                .retrieveKnowledge("他突然不回消息了怎么办");
        verify(loveApp, timeout(ASYNC_TIMEOUT_MS))
                .doChatWithRAGContextStream(
                        eq("他突然不回消息了怎么办"),
                        contains("恋爱中保持冷静"),
                        eq("user-1:loveapp:chat-1"));
        verify(sseEventHelper, timeout(ASYNC_TIMEOUT_MS))
                .send(any(), eq("rag_status"), contains("知识库"));
        verify(sseEventHelper, timeout(ASYNC_TIMEOUT_MS))
                .send(any(), eq("content"),
                        eq("建议先不要催他，观察 24 小时后再发一条轻松的消息。"));
        verify(ocrAgentService, never()).extract(anyString(), anyString(), anyString());
    }

    @Test
    void stream_textOnly_shouldNotSaveImageUrl_whenNoImage() {
        ChatInputContext context = new ChatInputContext(
                "user-1", "chat-1", ChatMode.LOVE, "测试消息", null);

        when(toolHintDetector.detect(context.userMessage())).thenReturn(false);
        when(probabilityKeywordDetector.detect(context.userMessage())).thenReturn(false);
        when(ragKnowledgeService.retrieveKnowledge(any())).thenReturn("");
        when(loveApp.doChatWithRAGContextStream(any(), any(), any())).thenReturn(Flux.just("回复"));

        orchestrator.stream(context, "run-2");

        // Wait for flow to complete, then assert no imageUrl save
        verify(sseEventHelper, timeout(ASYNC_TIMEOUT_MS)).send(any(), eq("content"), any());
        verify(databaseChatMemory, never()).setImageUrlOnLatestUserMessage(any(), any());
    }

    // --- 图片输入流程 ---

    @Test
    void stream_withImage_shouldSendOcrEventAndPersistImageUrl() {
        ChatInputContext context = new ChatInputContext(
                "user-1", "chat-2", ChatMode.LOVE, "这条消息什么意思",
                "/api/images/user-1/chat/screenshot.png");

        when(ocrAgentService.extract(context.imageUrl(), context.userId(), context.userMessage()))
                .thenReturn(OcrExtractionResult.of("对方：明天不用等我了", "截图显示对方想分开"));
        when(toolHintDetector.detect(context.userMessage())).thenReturn(false);
        when(probabilityKeywordDetector.detect(context.userMessage())).thenReturn(false);
        when(ragKnowledgeService.retrieveKnowledge("截图显示对方想分开 / 这条消息什么意思"))
                .thenReturn("分手信号解读知识。");
        when(loveApp.doChatWithRAGContextStream(any(), any(), any()))
                .thenReturn(Flux.just("这可能不是分手，先冷静分析。"));

        orchestrator.stream(context, "run-3");

        verify(sseEventHelper, timeout(ASYNC_TIMEOUT_MS))
                .send(any(), eq("intake_status"), contains("截图"));
        verify(sseEventHelper, timeout(ASYNC_TIMEOUT_MS))
                .send(any(), eq("ocr_result"), contains("截图识别完成"));
        verify(databaseChatMemory, timeout(ASYNC_TIMEOUT_MS))
                .setImageUrlOnLatestUserMessage(
                        eq("chat-2"), eq("/api/images/user-1/chat/screenshot.png"));
    }

    // --- RAG 空结果降级 ---

    @Test
    void stream_shouldNotInjectKnowledgeBlock_whenRagReturnsEmpty() {
        ChatInputContext context = new ChatInputContext(
                "user-1", "chat-3", ChatMode.LOVE, "不相关问题", null);

        when(toolHintDetector.detect(context.userMessage())).thenReturn(false);
        when(probabilityKeywordDetector.detect(context.userMessage())).thenReturn(false);
        when(ragKnowledgeService.retrieveKnowledge(any())).thenReturn("");
        when(loveApp.doChatWithRAGContextStream(any(), any(), any())).thenReturn(Flux.just("我来回答。"));

        orchestrator.stream(context, "run-4");

        ArgumentCaptor<String> systemContextCaptor = ArgumentCaptor.forClass(String.class);
        verify(loveApp, timeout(ASYNC_TIMEOUT_MS))
                .doChatWithRAGContextStream(any(), systemContextCaptor.capture(), any());
        assertFalse(systemContextCaptor.getValue().contains("相关知识参考"),
                "RAG 无结果时不应包含知识参考块");
    }

    // --- 异常处理 ---

    @Test
    void stream_shouldSendErrorEvent_whenOcrThrows() {
        ChatInputContext context = new ChatInputContext(
                "user-1", "chat-4", ChatMode.LOVE, "测试",
                "/api/images/user-1/chat/error.png");

        when(ocrAgentService.extract(context.imageUrl(), context.userId(), context.userMessage()))
                .thenThrow(new RuntimeException("模型调用超时"));

        orchestrator.stream(context, "run-5");

        verify(sseEventHelper, timeout(ASYNC_TIMEOUT_MS))
                .send(any(), eq("error"), contains("处理失败"));
        verify(loveApp, never()).doChatWithRAGContextStream(any(), any(), any());
    }

    @Test
    void stream_withImage_whenOcrFailed_shouldStillContinue() {
        ChatInputContext context = new ChatInputContext(
                "user-1", "chat-5", ChatMode.LOVE, "我该怎么回",
                "/api/images/user-1/chat/fail.png");

        when(ocrAgentService.extract(context.imageUrl(), context.userId(), context.userMessage()))
                .thenReturn(OcrExtractionResult.failed("模型未返回可读文本"));
        when(toolHintDetector.detect(context.userMessage())).thenReturn(false);
        when(probabilityKeywordDetector.detect(context.userMessage())).thenReturn(false);
        when(ragKnowledgeService.retrieveKnowledge(any())).thenReturn("兜底知识");
        when(loveApp.doChatWithRAGContextStream(any(), any(), any())).thenReturn(Flux.just("先根据文字描述回复。"));

        orchestrator.stream(context, "run-6");

        verify(ragKnowledgeService, timeout(ASYNC_TIMEOUT_MS)).retrieveKnowledge(contains("我该怎么回"));
        verify(loveApp, timeout(ASYNC_TIMEOUT_MS)).doChatWithRAGContextStream(any(), any(), any());
        verify(sseEventHelper, timeout(ASYNC_TIMEOUT_MS)).send(any(), eq("content"), eq("先根据文字描述回复。"));
    }
}
