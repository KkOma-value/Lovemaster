package org.example.springai_learn.ai.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeSimilarityUtilsTest {

    @Test
    void identicalStringsReturnOne() {
        assertThat(KnowledgeSimilarityUtils.jaccard("破冰期聊天技巧", "破冰期聊天技巧")).isEqualTo(1.0);
    }

    @Test
    void disjointStringsReturnZero() {
        assertThat(KnowledgeSimilarityUtils.jaccard("abcd", "wxyz")).isEqualTo(0.0);
    }

    @Test
    void nullsAndBlankAreSafe() {
        assertThat(KnowledgeSimilarityUtils.jaccard(null, null)).isEqualTo(0.0);
        assertThat(KnowledgeSimilarityUtils.jaccard("", "")).isEqualTo(0.0);
        assertThat(KnowledgeSimilarityUtils.jaccard("abc", null)).isEqualTo(0.0);
        assertThat(KnowledgeSimilarityUtils.isDuplicate(null, "abc", 0.8)).isFalse();
    }

    @Test
    void cjkBigramsOverlap() {
        double score = KnowledgeSimilarityUtils.jaccard("破冰期聊天怎么切入", "破冰阶段聊天如何切入");
        assertThat(score).isBetween(0.1, 0.7);
    }

    @Test
    void highSimilarityTriggersDuplicate() {
        assertThat(KnowledgeSimilarityUtils.isDuplicate("破冰期聊天技巧", "破冰期聊天技巧a", 0.85)).isTrue();
    }

    @Test
    void singleCharacterIsHandled() {
        assertThat(KnowledgeSimilarityUtils.shingles("a")).containsExactly("a");
    }
}
