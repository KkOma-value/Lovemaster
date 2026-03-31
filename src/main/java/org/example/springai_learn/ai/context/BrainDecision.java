package org.example.springai_learn.ai.context;

/**
 * BrainAgent 的决策结果。
 *
 * @param needsTools       是否需要激活 ToolsAgent
 * @param toolTaskPrompt   交给 ToolsAgent 执行的任务描述（needsTools=true 时有值）
 * @param directAnswer     Brain 直接回答的内容（needsTools=false 时有值）
 * @param synthesisContext Brain 用于综合工具结果的上下文（needsTools=true 时有值）
 * @param userFacingPrelude 给用户的过渡状态提示
 */
public record BrainDecision(
        boolean needsTools,
        String toolTaskPrompt,
        String directAnswer,
        String synthesisContext,
        String userFacingPrelude
) {

    /**
     * Brain 判断需要工具执行。
     */
    public static BrainDecision useTools(String toolTaskPrompt, String synthesisContext, String prelude) {
        return new BrainDecision(true, toolTaskPrompt, null, synthesisContext, prelude);
    }

    /**
     * Brain 直接回答，无需工具。
     */
    public static BrainDecision directAnswer(String answer, String synthesisContext, String prelude) {
        return new BrainDecision(false, null, answer, synthesisContext, prelude);
    }
}
