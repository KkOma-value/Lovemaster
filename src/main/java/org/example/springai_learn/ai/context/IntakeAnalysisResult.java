package org.example.springai_learn.ai.context;

import java.util.ArrayList;
import java.util.List;

public record IntakeAnalysisResult(
        boolean hasImage,
        String ocrText,
        String conversationSummary,
        String rewrittenQuestion,
        List<String> uncertainties,
        String suggestedIntent,
        boolean likelyNeedTools,
        boolean probabilityRequested
) {

    /**
     * 新链路主链路合成器：不再调用 RewriteAgent，rewrittenQuestion 直接等于用户原文。
     * summary / ocrText 来自 OcrAgent（可空）；likelyNeedTools / probabilityRequested 来自关键词 detector。
     * 下游 BrainAgent / ProbabilityAnalysisService 依赖的字段全部兜底填充，契约保持兼容。
     */
    public static IntakeAnalysisResult forMainChain(
            ChatInputContext context,
            OcrExtractionResult ocr,
            boolean likelyNeedTools,
            boolean probabilityRequested) {

        boolean hasImage = context.hasImage();
        String userMessage = context.userMessage() == null ? "" : context.userMessage();

        String ocrText = "";
        String summary;
        List<String> uncertainties = new ArrayList<>();

        if (hasImage && ocr != null) {
            ocrText = ocr.ocrText() == null ? "" : ocr.ocrText();
            if (ocr.success()) {
                summary = ocr.sceneSummary() == null || ocr.sceneSummary().isBlank()
                        ? "用户提供了聊天截图并附上文字描述，希望获得分析与回复建议。"
                        : ocr.sceneSummary();
            } else {
                summary = "用户提供了聊天截图，但系统未能完整识别，当前以用户文字描述为主继续分析。";
                uncertainties.add("截图识别未成功，回答基于用户的文字描述。");
            }
        } else {
            summary = "用户希望获得一段恋爱关系分析和回复建议。";
        }

        String intent = "理解当前关系状态并获得下一步建议";

        return new IntakeAnalysisResult(
                hasImage,
                ocrText,
                summary,
                userMessage,
                uncertainties,
                intent,
                likelyNeedTools,
                probabilityRequested
        );
    }
}
