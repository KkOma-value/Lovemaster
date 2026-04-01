package org.example.springai_learn.ai.orchestrator;

import org.example.springai_learn.ChatMemory.DatabaseChatMemory;
import org.example.springai_learn.ai.context.ChatInputContext;
import org.example.springai_learn.ai.context.ChatMode;
import org.example.springai_learn.ai.context.IntakeAnalysisResult;
import org.example.springai_learn.ai.service.ChatRunService;
import org.example.springai_learn.ai.service.MultimodalIntakeService;
import org.example.springai_learn.ai.service.RagKnowledgeService;
import org.example.springai_learn.ai.service.SseEventHelper;
import org.example.springai_learn.app.LoveApp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LoveChatOrchestratorTest {

    private static final int ASYNC_TIMEOUT_MS = 3000;

    private MultimodalIntakeService intakeService;
    private RagKnowledgeService ragKnowledgeService;
    private SseEventHelper sseEventHelper;
    private LoveApp loveApp;
    private DatabaseChatMemory databaseChatMemory;
    private ChatRunService chatRunService;
    private LoveChatOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        intakeService = mock(MultimodalIntakeService.class);
        ragKnowledgeService = mock(RagKnowledgeService.class);
        sseEventHelper = mock(SseEventHelper.class);
        loveApp = mock(LoveApp.class);
        databaseChatMemory = mock(DatabaseChatMemory.class);
        chatRunService = mock(ChatRunService.class);
        orchestrator = new LoveChatOrchestrator(intakeService, ragKnowledgeService,
                sseEventHelper, loveApp, databaseChatMemory, chatRunService);
    }

    // --- 文本输入流程 ---

    @Test
    void stream_textOnly_shouldCallRagAndReturnAnswer() {
        ChatInputContext context = new ChatInputContext(
                "user-1", "chat-1", ChatMode.LOVE, "他突然不回消息了怎么办", null);

        IntakeAnalysisResult analysis = new IntakeAnalysisResult(
                false, "", "对方回复变慢，用户焦虑",
                "对方为什么突然不回消息了", List.of(), "理解对方意图", false);

        when(intakeService.analyze(context)).thenReturn(analysis);
        when(ragKnowledgeService.retrieveKnowledge("对方为什么突然不回消息了"))
                .thenReturn("恋爱中保持冷静是关键。");
        when(loveApp.doChatWithRAGContextStream(any(), any(), eq("user-1:loveapp:chat-1")))
                .thenReturn(Flux.just("建议先不要催他，观察 24 小时后再发一条轻松的消息。"));

        orchestrator.stream(context, "run-1");

        verify(ragKnowledgeService, timeout(ASYNC_TIMEOUT_MS))
                .retrieveKnowledge("对方为什么突然不回消息了");
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
    }

    @Test
    void stream_textOnly_shouldNotSaveImageUrl_whenNoImage() {
        ChatInputContext context = new ChatInputContext(
                "user-1", "chat-1", ChatMode.LOVE, "测试消息", null);

        IntakeAnalysisResult analysis = new IntakeAnalysisResult(
                false, "", "摘要", "重写问题", List.of(), "意图", false);

        when(intakeService.analyze(context)).thenReturn(analysis);
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

        IntakeAnalysisResult analysis = new IntakeAnalysisResult(
                true, "对方：明天不用等我了", "截图显示对方想分开",
                "截图中的消息是什么意思，对方是否要分手",
                List.of(), "理解对方意图", false);

        when(intakeService.analyze(context)).thenReturn(analysis);
        when(ragKnowledgeService.retrieveKnowledge(any())).thenReturn("分手信号解读知识。");
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

        IntakeAnalysisResult analysis = new IntakeAnalysisResult(
                false, "", "摘要", "重写问题", List.of(), "意图", false);

        when(intakeService.analyze(context)).thenReturn(analysis);
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
    void stream_shouldSendErrorEvent_whenIntakeThrows() {
        ChatInputContext context = new ChatInputContext(
                "user-1", "chat-4", ChatMode.LOVE, "测试", null);

        when(intakeService.analyze(context)).thenThrow(new RuntimeException("模型调用超时"));

        orchestrator.stream(context, "run-5");

        verify(sseEventHelper, timeout(ASYNC_TIMEOUT_MS))
                .send(any(), eq("error"), contains("处理失败"));
        verify(loveApp, never()).doChatWithRAGContextStream(any(), any(), any());
    }
}
