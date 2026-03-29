package org.example.springai_learn.ai.service;

import org.example.springai_learn.ai.context.ChatInputContext;
import org.example.springai_learn.ai.context.CoachRoutingDecision;
import org.example.springai_learn.ai.context.IntakeAnalysisResult;
import org.springframework.stereotype.Service;

@Service
public class CoachRoutingService {

    public CoachRoutingDecision decide(ChatInputContext context, IntakeAnalysisResult analysis, String ragKnowledge) {
        boolean useTools = analysis.likelyNeedTools() || looksLikeTaskRequest(context.userMessage());

        String ragBlock = (ragKnowledge != null && !ragKnowledge.isBlank())
                ? "\n# 相关知识参考\n" + ragKnowledge + "\n"
                : "";

        String intakeBlock = """
                # 当前请求分析
                用户原始问题：%s
                对话摘要：%s
                OCR 摘录：%s
                重写后的问题：%s
                不确定项：%s
                用户意图：%s
                """.formatted(
                safe(context.userMessage()),
                safe(analysis.conversationSummary()),
                safe(analysis.ocrText()),
                safe(analysis.rewrittenQuestion()),
                analysis.uncertainties().isEmpty() ? "无" : String.join("；", analysis.uncertainties()),
                safe(analysis.suggestedIntent())
        ) + ragBlock;

        if (!useTools) {
            return new CoachRoutingDecision(
                    false,
                    intakeBlock + "\n请直接回答用户，先解释局势，再给出下一步建议；不要进入工具执行。",
                    "",
                    "我先把这段聊天整理清楚了，这件事暂时不需要进入任务执行，我直接给你判断。"
            );
        }

        return new CoachRoutingDecision(
                true,
                "",
                intakeBlock + "\n请把这次请求当成一个需要自主执行的恋爱任务，必要时调用工具，但最终要给用户清晰可用的结论。",
                "我先把截图和问题整理好了，这件事更适合进入任务执行，我继续帮你查和整理。"
        );
    }

    private boolean looksLikeTaskRequest(String message) {
        if (message == null) {
            return false;
        }
        String text = message.toLowerCase();
        return text.contains("查")
                || text.contains("搜")
                || text.contains("收集")
                || text.contains("规划")
                || text.contains("执行")
                || text.contains("整理成")
                || text.contains("给我一份")
                || text.contains("生成");
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }
}
