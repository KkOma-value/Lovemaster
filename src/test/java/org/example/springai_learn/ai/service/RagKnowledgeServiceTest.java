package org.example.springai_learn.ai.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.example.springai_learn.config.DifyProperties;
import org.example.springai_learn.config.KnowledgeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RagKnowledgeServiceTest {

    private DifyKnowledgeService difyKnowledgeService;
    private WikiKnowledgeService wikiKnowledgeService;
    private KnowledgeReranker knowledgeReranker;
    private RagKnowledgeService ragKnowledgeService;

    @BeforeEach
    void setUp() {
        difyKnowledgeService = mock(DifyKnowledgeService.class);
        wikiKnowledgeService = mock(WikiKnowledgeService.class);
        knowledgeReranker = mock(KnowledgeReranker.class);
        when(knowledgeReranker.rerankAndDedup(anyString(), anyString()))
                .thenAnswer(inv -> inv.getArgument(1));

        DifyProperties difyProperties = new DifyProperties();
        difyProperties.getRetrieve().setTopK(4);

        KnowledgeProperties knowledgeProperties = new KnowledgeProperties();
        knowledgeProperties.getWiki().getRetrieval().setScoreThreshold(0.6);
        knowledgeProperties.getWiki().getRetrieval().setTimeoutMs(200);
        knowledgeProperties.getFanout().setTotalTimeoutMs(800);
        knowledgeProperties.getCache().setEnabled(true);
        knowledgeProperties.getCache().setTtlSeconds(300);
        knowledgeProperties.getCache().setMaximumSize(100);
        knowledgeProperties.getRerank().setEnabled(false);
        knowledgeProperties.getSanitizer().setEnabled(true);

        KnowledgeMetrics metrics = new KnowledgeMetrics(new SimpleMeterRegistry());
        KnowledgePromptSanitizer sanitizer = new KnowledgePromptSanitizer(knowledgeProperties);
        Cache<String, String> cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(300))
                .maximumSize(100)
                .build();

        ragKnowledgeService = new RagKnowledgeService(
                difyKnowledgeService,
                wikiKnowledgeService,
                difyProperties,
                knowledgeProperties,
                Runnable::run,
                knowledgeReranker,
                sanitizer,
                metrics,
                cache
        );
    }

    @Test
    void retrieveKnowledge_shouldMergeWikiAndDify_whenWikiIsQualified() {
        when(wikiKnowledgeService.retrieveKnowledge("测试问题"))
                .thenReturn(new WikiKnowledgeService.WikiKnowledgeResult("Wiki块1\n---\nWiki块2", 0.9, 2));
        when(difyKnowledgeService.retrieveKnowledge("测试问题")).thenReturn("Dify块1\n---\nDify块2");

        String result = ragKnowledgeService.retrieveKnowledge("测试问题");

        assertTrue(result.startsWith("Wiki块1\n---\nWiki块2"));
        assertTrue(result.contains("Dify块1"));
        verify(wikiKnowledgeService).retrieveKnowledge("测试问题");
        verify(difyKnowledgeService).retrieveKnowledge("测试问题");
    }

    @Test
    void retrieveKnowledge_shouldPreferDify_whenWikiScoreIsLow() {
        when(wikiKnowledgeService.retrieveKnowledge("模糊问题"))
                .thenReturn(new WikiKnowledgeService.WikiKnowledgeResult("Wiki低分", 0.2, 1));
        when(difyKnowledgeService.retrieveKnowledge("模糊问题")).thenReturn("Dify主结果");

        String result = ragKnowledgeService.retrieveKnowledge("模糊问题");

        assertEquals("Dify主结果", result);
    }

    @Test
    void retrieveKnowledge_shouldReturnWikiFallback_whenDifyEmpty() {
        when(wikiKnowledgeService.retrieveKnowledge("本地命中问题"))
                .thenReturn(new WikiKnowledgeService.WikiKnowledgeResult("Wiki结果", 0.75, 1));
        when(difyKnowledgeService.retrieveKnowledge("本地命中问题")).thenReturn("");

        String result = ragKnowledgeService.retrieveKnowledge("本地命中问题");

        assertEquals("Wiki结果", result);
    }

    @Test
    void retrieveKnowledge_shouldReturnEmpty_whenBothBranchesMiss() {
        when(wikiKnowledgeService.retrieveKnowledge("无结果问题"))
                .thenReturn(WikiKnowledgeService.WikiKnowledgeResult.empty());
        when(difyKnowledgeService.retrieveKnowledge("无结果问题")).thenReturn("");

        String result = ragKnowledgeService.retrieveKnowledge("无结果问题");

        assertEquals("", result);
        verify(wikiKnowledgeService).retrieveKnowledge("无结果问题");
        verify(difyKnowledgeService).retrieveKnowledge("无结果问题");
    }

    @Test
    void retrieveKnowledge_shouldHitCache_onRepeatedQuery() {
        when(wikiKnowledgeService.retrieveKnowledge("缓存问题"))
                .thenReturn(new WikiKnowledgeService.WikiKnowledgeResult("Wiki缓存结果", 0.9, 1));
        when(difyKnowledgeService.retrieveKnowledge("缓存问题")).thenReturn("");

        String first = ragKnowledgeService.retrieveKnowledge("缓存问题");
        String second = ragKnowledgeService.retrieveKnowledge("缓存问题");

        assertEquals(first, second);
        verify(wikiKnowledgeService, times(1)).retrieveKnowledge("缓存问题");
        verify(difyKnowledgeService, times(1)).retrieveKnowledge("缓存问题");
    }

    @Test
    void retrieveKnowledge_shouldInvokeReranker() {
        when(wikiKnowledgeService.retrieveKnowledge(anyString()))
                .thenReturn(new WikiKnowledgeService.WikiKnowledgeResult("A\n---\nB", 0.9, 2));
        when(difyKnowledgeService.retrieveKnowledge(anyString())).thenReturn("");

        ragKnowledgeService.retrieveKnowledge("重排问题");

        verify(knowledgeReranker).rerankAndDedup(eq("重排问题"), any());
    }
}
