package org.example.springai_learn.ai.context;

import java.util.List;

/**
 * 成功概率分析结构化结果。
 * 由 ProbabilityAnalysisService 通过 Spring AI 结构化输出产生。
 *
 * @param probability  成功概率 (0-100)
 * @param tier         分档: 极低 | 偏低 | 一般 | 较高 | 很高
 * @param confidence   置信度: low | medium | high
 * @param summary      1-2 句口语化总结
 * @param greenFlags   正面信号列表 (≥2)
 * @param redFlags     风险信号列表 (≥1)
 * @param nextActions  下一步行动建议列表 (=3)
 */
public record ProbabilityAnalysis(
        int probability,
        String tier,
        String confidence,
        String summary,
        List<Flag> greenFlags,
        List<Flag> redFlags,
        List<NextAction> nextActions
) {
    public record Flag(String title, String evidence, String weight) {}
    public record NextAction(String action, String tone) {}
}
