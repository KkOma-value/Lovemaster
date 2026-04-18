package org.example.springai_learn.ai.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.example.springai_learn.config.KnowledgeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KnowledgeRerankerTest {

    private EmbeddingModel embeddingModel;
    private KnowledgeProperties properties;
    private KnowledgeReranker reranker;

    @BeforeEach
    void setUp() {
        embeddingModel = mock(EmbeddingModel.class);

        @SuppressWarnings("unchecked")
        ObjectProvider<EmbeddingModel> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(embeddingModel);

        properties = new KnowledgeProperties();
        properties.getRerank().setEnabled(true);
        properties.getRerank().setDedupThreshold(0.95);
        properties.getRerank().setTopK(2);
        properties.getRerank().setTimeoutMs(500);
        properties.getRerank().setMaxSegments(10);

        reranker = new KnowledgeReranker(
                provider,
                properties,
                Runnable::run,
                new KnowledgeMetrics(new SimpleMeterRegistry())
        );
    }

    @Test
    void rerank_shouldReorderByCosineSimilarity() {
        String query = "Q";
        String merged = "segA\n---\nsegB\n---\nsegC";
        when(embeddingModel.embed(any(List.class))).thenReturn(List.of(
                new float[]{1f, 0f},   // query
                new float[]{0f, 1f},   // segA - far
                new float[]{1f, 0f},   // segB - matches query
                new float[]{0.5f, 0.5f} // segC - medium
        ));

        String result = reranker.rerankAndDedup(query, merged);

        assertTrue(result.startsWith("segB"), "segB should rank first; got: " + result);
    }

    @Test
    void rerank_shouldDedupNearDuplicates() {
        String query = "Q";
        String merged = "first\n---\nduplicate\n---\nunique";
        when(embeddingModel.embed(any(List.class))).thenReturn(List.of(
                new float[]{1f, 0f},
                new float[]{0.9f, 0.1f},
                new float[]{0.9f, 0.1f},
                new float[]{0f, 1f}
        ));

        properties.getRerank().setTopK(3);
        String result = reranker.rerankAndDedup(query, merged);

        long separators = result.split("\\n---\\n", -1).length - 1;
        assertEquals(1, separators, "Expected 2 kept segments (one dedup dropped), got: " + result);
    }

    @Test
    void rerank_shouldBeNoOpWhenDisabled() {
        properties.getRerank().setEnabled(false);
        String merged = "a\n---\nb";

        assertEquals(merged, reranker.rerankAndDedup("Q", merged));
        verifyNoInteractions(embeddingModel);
    }

    @Test
    void rerank_shouldFallbackWhenEmbeddingFails() {
        String merged = "a\n---\nb";
        when(embeddingModel.embed(any(List.class))).thenThrow(new RuntimeException("boom"));

        assertEquals(merged, reranker.rerankAndDedup("Q", merged));
    }

    @Test
    void rerank_shouldReturnSingleSegmentUntouched() {
        String merged = "only one";
        assertEquals(merged, reranker.rerankAndDedup("Q", merged));
        verifyNoInteractions(embeddingModel);
    }
}
