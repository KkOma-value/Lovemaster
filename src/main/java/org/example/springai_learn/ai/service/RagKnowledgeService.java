package org.example.springai_learn.ai.service;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.config.DifyProperties;
import org.example.springai_learn.config.KnowledgeProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * RAG 知识检索服务。
 * 接收重写后的查询文本，并行从本地 Wiki 与 Dify 检索相关文档片段，
 * 再经过 embedding 重排去重、Caffeine 缓存、prompt-injection 过滤，
 * 最终注入 system prompt 增强回答质量。
 */
@Service
@Slf4j
public class RagKnowledgeService {

    private static final String SEGMENT_SEPARATOR = "\n---\n";

    private final DifyKnowledgeService difyKnowledgeService;
    private final WikiKnowledgeService wikiKnowledgeService;
    private final DifyProperties difyProperties;
    private final KnowledgeProperties knowledgeProperties;
    private final Executor knowledgeExecutor;
    private final KnowledgeReranker knowledgeReranker;
    private final KnowledgePromptSanitizer knowledgePromptSanitizer;
    private final KnowledgeMetrics knowledgeMetrics;
    private final Cache<String, String> knowledgeRetrievalCache;

    public RagKnowledgeService(DifyKnowledgeService difyKnowledgeService,
                               WikiKnowledgeService wikiKnowledgeService,
                               DifyProperties difyProperties,
                               KnowledgeProperties knowledgeProperties,
                               @Qualifier("knowledgeExecutor") Executor knowledgeExecutor,
                               KnowledgeReranker knowledgeReranker,
                               KnowledgePromptSanitizer knowledgePromptSanitizer,
                               KnowledgeMetrics knowledgeMetrics,
                               @Qualifier("knowledgeRetrievalCache") Cache<String, String> knowledgeRetrievalCache) {
        this.difyKnowledgeService = difyKnowledgeService;
        this.wikiKnowledgeService = wikiKnowledgeService;
        this.difyProperties = difyProperties;
        this.knowledgeProperties = knowledgeProperties;
        this.knowledgeExecutor = knowledgeExecutor;
        this.knowledgeReranker = knowledgeReranker;
        this.knowledgePromptSanitizer = knowledgePromptSanitizer;
        this.knowledgeMetrics = knowledgeMetrics;
        this.knowledgeRetrievalCache = knowledgeRetrievalCache;
    }

    /**
     * 根据查询词从恋爱知识库检索相关内容。
     *
     * @param query 查询文本，通常为 IntakeAnalysisResult.rewrittenQuestion()
     * @return 格式化的知识片段字符串，无匹配结果时返回空字符串
     */
    public String retrieveKnowledge(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }

        boolean cacheEnabled = knowledgeProperties.getCache().isEnabled();
        String cacheKey = cacheEnabled ? cacheKey(query) : null;
        if (cacheEnabled) {
            String cached = knowledgeRetrievalCache.getIfPresent(cacheKey);
            if (cached != null) {
                knowledgeMetrics.cacheHit();
                log.debug("Knowledge cache hit, chars={}", cached.length());
                return cached;
            }
            knowledgeMetrics.cacheMiss();
        }

        long startNanos = System.nanoTime();
        CompletableFuture<WikiKnowledgeService.WikiKnowledgeResult> wikiFuture = CompletableFuture
                .supplyAsync(() -> wikiKnowledgeService.retrieveKnowledge(query), knowledgeExecutor)
                .completeOnTimeout(
                        WikiKnowledgeService.WikiKnowledgeResult.empty(),
                        Math.max(1, knowledgeProperties.getWiki().getRetrieval().getTimeoutMs()),
                        TimeUnit.MILLISECONDS
                )
                .exceptionally(ex -> {
                    log.warn("Wiki retrieval failed: {}", ex.getMessage());
                    return WikiKnowledgeService.WikiKnowledgeResult.empty();
                });

        CompletableFuture<String> difyFuture = CompletableFuture
                .supplyAsync(() -> difyKnowledgeService.retrieveKnowledge(query), knowledgeExecutor)
                .exceptionally(ex -> {
                    log.warn("Dify retrieval failed in fanout: {}", ex.getMessage());
                    return "";
                });

        CompletableFuture.allOf(wikiFuture, difyFuture)
                .completeOnTimeout(null, Math.max(1, knowledgeProperties.getFanout().getTotalTimeoutMs()), TimeUnit.MILLISECONDS)
                .join();

        WikiKnowledgeService.WikiKnowledgeResult wikiResult = wikiFuture.getNow(WikiKnowledgeService.WikiKnowledgeResult.empty());
        String difyResult = difyFuture.getNow("");
        MergeResult mergeResult = merge(wikiResult, difyResult);

        if (wikiResult != null && wikiResult.hasContent()) {
            knowledgeMetrics.recordHit("wiki");
        }
        if (difyResult != null && !difyResult.isBlank()) {
            knowledgeMetrics.recordHit("dify");
        }

        String reranked = knowledgeReranker.rerankAndDedup(query, mergeResult.content());
        KnowledgePromptSanitizer.SanitizeResult sanitized = knowledgePromptSanitizer.sanitize(reranked);
        knowledgeMetrics.sanitizerBlocked(sanitized.blockedHits());
        String finalContent = sanitized.content();

        long elapsedNanos = System.nanoTime() - startNanos;
        knowledgeMetrics.recordFanout(mergeResult.strategy(), elapsedNanos);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
        log.info(
                "Knowledge fanout completed in {} ms, wikiHit={}, wikiTopScore={}, mergeStrategy={}, rerankedChars={}, sanitizedBlocked={}",
                elapsedMs,
                wikiResult.hitCount(),
                wikiResult.topScore(),
                mergeResult.strategy(),
                finalContent.length(),
                sanitized.blockedHits()
        );

        if (cacheEnabled && !finalContent.isBlank() && !"both-miss".equals(mergeResult.strategy())) {
            knowledgeRetrievalCache.put(cacheKey, finalContent);
        }
        return finalContent;
    }

    private MergeResult merge(WikiKnowledgeService.WikiKnowledgeResult wikiResult, String difyResult) {
        boolean hasWiki = wikiResult != null && wikiResult.hasContent();
        boolean hasDify = difyResult != null && !difyResult.isBlank();

        if (!hasWiki && !hasDify) {
            return new MergeResult("", "both-miss");
        }

        double threshold = knowledgeProperties.getWiki().getRetrieval().getScoreThreshold();
        boolean wikiQualified = hasWiki && wikiResult.topScore() >= threshold;

        if (wikiQualified) {
            if (!hasDify) {
                return new MergeResult(wikiResult.content(), "wiki-only");
            }
            String merged = mergeWikiFirst(wikiResult.content(), difyResult);
            return new MergeResult(merged, "merged");
        }

        if (hasDify) {
            return new MergeResult(difyResult, hasWiki ? "dify-primary-low-wiki" : "dify-only");
        }

        return new MergeResult(wikiResult.content(), "wiki-fallback");
    }

    private String mergeWikiFirst(String wikiContent, String difyContent) {
        List<String> wikiSegments = splitSegments(wikiContent);
        List<String> difySegments = splitSegments(difyContent);

        int targetCount = Math.max(wikiSegments.size(), Math.max(1, difyProperties.getRetrieve().getTopK()));
        LinkedHashSet<String> merged = new LinkedHashSet<>();

        for (String segment : wikiSegments) {
            if (!segment.isBlank()) {
                merged.add(segment);
            }
            if (merged.size() >= targetCount) {
                break;
            }
        }

        for (String segment : difySegments) {
            if (!segment.isBlank()) {
                merged.add(segment);
            }
            if (merged.size() >= targetCount) {
                break;
            }
        }

        return String.join(SEGMENT_SEPARATOR, merged);
    }

    private List<String> splitSegments(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        String[] rawSegments = content.split("\\n---\\n");
        List<String> segments = new ArrayList<>(rawSegments.length);
        for (String raw : rawSegments) {
            String normalized = raw == null ? "" : raw.trim();
            if (!normalized.isBlank()) {
                segments.add(normalized);
            }
        }
        return segments;
    }

    private String cacheKey(String query) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(query.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(query.hashCode());
        }
    }

    private record MergeResult(String content, String strategy) {
    }
}
