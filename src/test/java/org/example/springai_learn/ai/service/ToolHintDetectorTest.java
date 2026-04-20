package org.example.springai_learn.ai.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolHintDetectorTest {

    private final ToolHintDetector detector = new ToolHintDetector();

    @Test
    void detect_shouldReturnTrue_whenToolKeywordsPresent() {
        assertTrue(detector.detect("帮我查一下上海约会地点"));
        assertTrue(detector.detect("推荐一个旅行计划"));
    }

    @Test
    void detect_shouldReturnFalse_whenNoToolIntent() {
        assertFalse(detector.detect("她说晚安是不是在敷衍我"));
        assertFalse(detector.detect(""));
        assertFalse(detector.detect(null));
    }
}
