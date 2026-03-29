package org.example.springai_learn.ai.context;

public record CoachRoutingDecision(
        boolean shouldUseTools,
        String directAnswerPrompt,
        String toolTaskPrompt,
        String userFacingPrelude
) {
}
