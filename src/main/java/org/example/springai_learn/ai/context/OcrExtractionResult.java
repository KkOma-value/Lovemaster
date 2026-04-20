package org.example.springai_learn.ai.context;

/**
 * OcrAgent 抽取结果。职责单一：只承载聊天截图的 OCR 文字摘录 + 场景概述，
 * 不包含"重写后的用户问题"这类主观改写内容。
 */
public record OcrExtractionResult(
        String ocrText,
        String sceneSummary,
        boolean success
) {

    public static OcrExtractionResult of(String ocrText, String sceneSummary) {
        return new OcrExtractionResult(
                ocrText == null ? "" : ocrText,
                sceneSummary == null ? "" : sceneSummary,
                true
        );
    }

    public static OcrExtractionResult failed(String reason) {
        String placeholder = reason == null || reason.isBlank()
                ? "截图暂未识别成功，当前先根据你的文字描述继续分析。"
                : "截图识别失败：" + reason;
        return new OcrExtractionResult(placeholder, "", false);
    }
}
