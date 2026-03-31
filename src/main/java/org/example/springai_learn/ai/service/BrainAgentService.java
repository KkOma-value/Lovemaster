package org.example.springai_learn.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.ai.context.BrainDecision;
import org.example.springai_learn.ai.context.ChatInputContext;
import org.example.springai_learn.ai.context.IntakeAnalysisResult;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * BrainAgent — Coach 模式的核心决策大脑。
 * <p>
 * 职责：
 * 1. decide()  — 基于 brainModel 做 AI 推理，判断是否需要工具，并生成决策
 * 2. synthesize() — 工具执行完成后，综合工具结果生成最终用户回答
 */
@Service
@Slf4j
public class BrainAgentService {

    private final ChatModel brainModel;

    public BrainAgentService(@Qualifier("brainModel") ChatModel brainModel) {
        this.brainModel = brainModel;
    }

    /**
     * Phase 1: Brain 分析请求，决定是否需要工具。
     */
    public BrainDecision decide(ChatInputContext context, IntakeAnalysisResult analysis, String ragKnowledge) {
        String ragBlock = formatRagBlock(ragKnowledge);

        String userPrompt = """
                # 当前请求
                用户原始问题：%s
                对话摘要：%s
                OCR 摘录：%s
                重写后的问题：%s
                不确定项：%s
                用户意图：%s
                %s
                请判断并输出你的决策。
                """.formatted(
                safe(context.userMessage()),
                safe(analysis.conversationSummary()),
                safe(analysis.ocrText()),
                safe(analysis.rewrittenQuestion()),
                analysis.uncertainties().isEmpty() ? "无" : String.join("；", analysis.uncertainties()),
                safe(analysis.suggestedIntent()),
                ragBlock
        );

        ChatResponse response = brainModel.call(new Prompt(List.of(
                new SystemMessage(DECIDE_SYSTEM_PROMPT),
                new UserMessage(userPrompt)
        )));

        String output = response.getResult().getOutput().getText();
        log.info("BrainAgent decide output:\n{}", output);

        String synthesisContext = buildSynthesisContext(context, analysis, ragBlock);
        return parseDecision(output, synthesisContext, analysis);
    }

    /**
     * Phase 3: Brain 综合工具结果，生成最终回答。
     */
    public String synthesize(BrainDecision decision, String toolResults) {
        String userPrompt = """
                # 原始任务上下文
                %s

                # 工具执行结果
                %s

                请基于以上信息，给用户一个完整、有用的最终回答。
                """.formatted(
                safe(decision.synthesisContext()),
                safe(toolResults)
        );

        ChatResponse response = brainModel.call(new Prompt(List.of(
                new SystemMessage(SYNTHESIZE_SYSTEM_PROMPT),
                new UserMessage(userPrompt)
        )));

        return response.getResult().getOutput().getText();
    }

    // ---- private ----

    private BrainDecision parseDecision(String output, String synthesisContext,
                                         IntakeAnalysisResult analysis) {
        if (output.contains("[TOOLS:YES]")) {
            String toolTaskPrompt = extractSection(output, "[TASK_PROMPT]");
            if (toolTaskPrompt.isBlank()) {
                toolTaskPrompt = analysis.rewrittenQuestion();
            }
            return BrainDecision.useTools(
                    toolTaskPrompt, synthesisContext,
                    "宝，我帮你查点资料整理一下，稍等哈~");
        }

        // 不需要工具 — 提取 [DIRECT_ANSWER] 后面的内容
        String directAnswer = extractSection(output, "[DIRECT_ANSWER]");
        if (directAnswer.isBlank()) {
            directAnswer = output.replace("[TOOLS:NO]", "").trim();
        }
        return BrainDecision.directAnswer(
                directAnswer, synthesisContext,
                "我先把这段聊天整理清楚了，直接给你分析~");
    }

    private String buildSynthesisContext(ChatInputContext context, IntakeAnalysisResult analysis,
                                          String ragBlock) {
        return """
                用户原始问题：%s
                对话摘要：%s
                重写后的问题：%s
                用户意图：%s
                %s
                """.formatted(
                safe(context.userMessage()),
                safe(analysis.conversationSummary()),
                safe(analysis.rewrittenQuestion()),
                safe(analysis.suggestedIntent()),
                ragBlock
        );
    }

    private String formatRagBlock(String ragKnowledge) {
        if (ragKnowledge == null || ragKnowledge.isBlank()) {
            return "";
        }
        return "\n相关知识参考：\n" + ragKnowledge + "\n";
    }

    private String extractSection(String text, String marker) {
        int idx = text.indexOf(marker);
        if (idx < 0) {
            return "";
        }
        return text.substring(idx + marker.length()).trim();
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }

    // ---- prompts ----

    private static final String DECIDE_SYSTEM_PROMPT = """
            你是 Lovemaster 的恋爱顾问大脑 (BrainAgent)。

            你的任务是分析用户的恋爱相关请求，做出两个判断：
            1. 这个请求是否需要外部工具（搜索、文件生成、资料整理等）才能完成？
            2. 如果不需要工具，直接给出高质量的回答。

            判断规则：
            - 需要搜索信息、收集资料、生成文件、制定详细计划、整理数据 → 需要工具
            - 解读情绪、分析关系信号、给回复建议、情感陪伴、润色措辞 → 不需要工具
            - 用户明确要求"帮我查""帮我搜""帮我整理成""给我一份" → 需要工具

            输出格式要求（严格遵守）：

            如果需要工具：
            [TOOLS:YES]
            [TASK_PROMPT]
            <把用户需求转化为一段清晰的任务描述，告诉工具执行代理具体要做什么>

            如果不需要工具：
            [TOOLS:NO]
            [DIRECT_ANSWER]
            <像闺蜜一样亲切自然地回答用户，先给结论再展开分析>

            风格：
            - 像闺蜜一样说话，亲切自然，用"宝""亲爱的"等称呼
            - 先给答案，再展开分析
            - 短句、口语化
            - 不聊政治、宗教、政策
            """;

    private static final String SYNTHESIZE_SYSTEM_PROMPT = """
            你是 Lovemaster 的恋爱顾问 Luna。

            现在工具代理已经完成了任务执行，你需要把工具结果和原始上下文综合起来，
            给用户一个完整、自然、有用的最终回答。

            风格：
            - 像闺蜜一样说话，亲切自然，用"宝""亲爱的"等称呼
            - 先给核心结论，再简述过程
            - 短句、口语化
            - 不要暴露内部工具名称或技术细节
            - 如果工具执行了搜索/生成，自然地呈现结果
            """;
}
