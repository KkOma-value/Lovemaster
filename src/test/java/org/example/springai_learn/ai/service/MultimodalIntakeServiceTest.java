package org.example.springai_learn.ai.service;

import org.example.springai_learn.ai.context.ChatInputContext;
import org.example.springai_learn.ai.context.ChatMode;
import org.example.springai_learn.ai.context.IntakeAnalysisResult;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MultimodalIntakeServiceTest {

    @Test
    void analyze_shouldFallbackLocally_whenTextRewriteModelTimesOut() {
        ChatModel rewriteModel = mock(ChatModel.class);
        when(rewriteModel.call(any(Prompt.class))).thenThrow(new RuntimeException("504 Gateway Timeout"));

        MultimodalIntakeService service = new MultimodalIntakeService(rewriteModel);
        ChatInputContext context = new ChatInputContext(
                "user-1",
                "chat-1",
                ChatMode.COACH,
                "我想要跟我对象去北京玩，你有什么经典推荐嘛？如果可以的话给我照片看看",
                null
        );

        IntakeAnalysisResult result = service.analyze(context);

        assertFalse(result.conversationSummary().isBlank());
        assertTrue(result.rewrittenQuestion().contains("整理出需要查询或推荐的信息"));
        assertTrue(result.likelyNeedTools());
        assertTrue(result.uncertainties().stream()
                .anyMatch(item -> item.contains("本地兜底分析")));
    }
}
