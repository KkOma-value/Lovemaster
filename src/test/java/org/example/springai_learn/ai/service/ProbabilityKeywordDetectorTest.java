package org.example.springai_learn.ai.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProbabilityKeywordDetectorTest {

    private final ProbabilityKeywordDetector detector = new ProbabilityKeywordDetector();

    @Test
    void detect_shouldReturnTrue_whenProbabilityKeywordsPresent() {
        assertTrue(detector.detect("她对我有没有戏？"));
        assertTrue(detector.detect("帮我分析一下成功概率"));
    }

    @Test
    void detect_shouldReturnFalse_whenNoProbabilityIntent() {
        assertFalse(detector.detect("今天怎么开场白比较自然"));
        assertFalse(detector.detect(""));
        assertFalse(detector.detect(null));
    }
}
