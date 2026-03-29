package org.example.springai_learn.ai.service;

import org.example.springai_learn.ai.context.ChatInputContext;
import org.example.springai_learn.ai.context.ChatMode;
import org.example.springai_learn.ai.context.CoachRoutingDecision;
import org.example.springai_learn.ai.context.IntakeAnalysisResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CoachRoutingServiceTest {

    private final CoachRoutingService coachRoutingService = new CoachRoutingService();

    @Test
    void decide_shouldStayInDirectAnswerModeForOrdinaryRelationshipQuestion() {
        ChatInputContext context = new ChatInputContext(
                "user-1",
                "chat-1",
                ChatMode.COACH,
                "他突然回得很冷淡，我现在怎么回比较稳妥？",
                null
        );
        IntakeAnalysisResult analysis = new IntakeAnalysisResult(
                false,
                "",
                "对方最近回复变慢，用户想判断对方态度。",
                "请判断对方目前的情绪和意图，并给我下一条更稳妥的回复。",
                List.of("缺少更早的聊天上下文"),
                "理解对方意思并获得回复建议",
                false
        );

        CoachRoutingDecision decision = coachRoutingService.decide(context, analysis, "");

        assertFalse(decision.shouldUseTools());
        assertTrue(decision.directAnswerPrompt().contains("不要进入工具执行"));
        assertTrue(decision.userFacingPrelude().contains("暂时不需要进入任务执行"));
    }

    @Test
    void decide_shouldEnterToolModeForPlanningOrSearchRequest() {
        ChatInputContext context = new ChatInputContext(
                "user-1",
                "chat-2",
                ChatMode.COACH,
                "你帮我查一下异地恋见面怎么安排，再整理成三天推进计划",
                "/api/images/user-1/chat/mock.png"
        );
        IntakeAnalysisResult analysis = new IntakeAnalysisResult(
                true,
                "我们下个月可能见面，对方说看你安排。",
                "用户希望基于截图内容继续查资料并生成推进方案。",
                "请结合截图，给我一份异地恋见面三天推进计划。",
                List.of(),
                "搜索并生成计划",
                true
        );

        CoachRoutingDecision decision = coachRoutingService.decide(context, analysis, "");

        assertTrue(decision.shouldUseTools());
        assertTrue(decision.toolTaskPrompt().contains("需要自主执行的恋爱任务"));
        assertTrue(decision.userFacingPrelude().contains("进入任务执行"));
    }
}
