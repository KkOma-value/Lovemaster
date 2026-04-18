package org.example.springai_learn.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.config.KnowledgeProperties;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * 基于 embedding 相似度的去重 + 重排。
 * 输入：query、合并后的知识 segment 列表；
 * 输出：按与 query 的余弦相似度排序、并剔除冗余的 TopK segment 列表。
 * 任一环节失败（超时 / 无 EmbeddingModel）时返回原始顺序，不阻塞主链路。
 */
@Service
@Slf4j
public class KnowledgeReranker {

    private static final String SEGMENT_SEPARATOR = "\n---\n";

    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final KnowledgeProperties properties;
    private final Executor executor;
    private final KnowledgeMetrics metrics;

    public KnowledgeReranker(ObjectProvider<EmbeddingModel> embeddingModelProvider,
                             KnowledgeProperties properties,
                             @Qualifier("knowledgeExecutor") Executor executor,
                             KnowledgeMetrics metrics) {
        this.embeddingModelProvider = embeddingModelProvider;
        this.properties = properties;
        this.executor = executor;
        this.metrics = metrics;
    }

    public String rerankAndDedup(String query, String mergedContent) {
        KnowledgeProperties.Rerank config = properties.getRerank();
        if (!config.isEnabled() || mergedContent == null || mergedContent.isBlank()) {
            return mergedContent == null ? "" : mergedContent;
        }

        List<String> segments = splitSegments(mergedContent);
        if (segments.size() <= 1) {
            return mergedContent;
        }

        int cap = Math.max(2, config.getMaxSegments());
        if (segments.size() > cap) {
            segments = new ArrayList<>(segments.subList(0, cap));
        }

        EmbeddingModel model = embeddingModelProvider.getIfAvailable();
        if (model == null) {
            log.debug("Embedding model not available, skipping rerank");
            return mergedContent;
        }

        final List<String> finalSegments = segments;
        final String safeQuery = query == null ? "" : query;
        CompletableFuture<List<float[]>> future = CompletableFuture.supplyAsync(() -> {
            List<String> texts = new ArrayList<>(finalSegments.size() + 1);
            texts.add(safeQuery);
            texts.addAll(finalSegments);
            return model.embed(texts);
        }, executor);

        List<float[]> vectors;
        try {
            vectors = future.get(Math.max(50, config.getTimeoutMs()), TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            log.warn("Embedding rerank skipped due to failure: {}", ex.getMessage());
            return mergedContent;
        }

        if (vectors == null || vectors.size() != finalSegments.size() + 1) {
            return mergedContent;
        }

        float[] queryVector = vectors.get(0);
        List<Scored> scored = new ArrayList<>(finalSegments.size());
        for (int i = 0; i < finalSegments.size(); i++) {
            scored.add(new Scored(finalSegments.get(i), vectors.get(i + 1), cosine(queryVector, vectors.get(i + 1))));
        }
        scored.sort((a, b) -> Float.compare(b.score, a.score));

        double dedupThreshold = config.getDedupThreshold();
        int topK = Math.max(1, config.getTopK());
        List<Scored> kept = new ArrayList<>(topK);
        int dropped = 0;
        for (Scored candidate : scored) {
            boolean duplicate = false;
            for (Scored existing : kept) {
                if (cosine(candidate.vector, existing.vector) >= dedupThreshold) {
                    duplicate = true;
                    break;
                }
            }
            if (duplicate) {
                dropped++;
                continue;
            }
            kept.add(candidate);
            if (kept.size() >= topK) {
                break;
            }
        }

        metrics.dedupDropped(dropped);
        log.debug("Rerank kept={}, dropped={}, topScore={}, worstKeptScore={}",
                kept.size(),
                dropped,
                scored.isEmpty() ? 0f : scored.get(0).score,
                kept.isEmpty() ? 0f : kept.get(kept.size() - 1).score);

        StringBuilder sb = new StringBuilder(mergedContent.length());
        for (int i = 0; i < kept.size(); i++) {
            if (i > 0) sb.append(SEGMENT_SEPARATOR);
            sb.append(kept.get(i).text);
        }
        return sb.toString();
    }

    private static float cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || a.length != b.length) {
            return 0f;
        }
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0f;
        return (float) (dot / (Math.sqrt(na) * Math.sqrt(nb)));
    }

    private static List<String> splitSegments(String content) {
        if (content == null || content.isBlank()) return List.of();
        String[] raw = content.split("\\n---\\n");
        List<String> out = new ArrayList<>(raw.length);
        for (String r : raw) {
            if (r != null) {
                String trimmed = r.trim();
                if (!trimmed.isBlank()) out.add(trimmed);
            }
        }
        return out;
    }

    private record Scored(String text, float[] vector, float score) {
    }
}
