package org.example.springai_learn.ai.context;

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
}
