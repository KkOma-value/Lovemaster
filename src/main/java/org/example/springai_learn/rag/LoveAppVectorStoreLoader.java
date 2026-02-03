package org.example.springai_learn.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 负责在应用启动后异步加载文档到 VectorStore。
 * 支持配置开关和有界重试。
 */
@Component
@Slf4j
public class LoveAppVectorStoreLoader {

    private final ApplicationContext applicationContext;
    private final LoveAppDocumentLoader documentLoader;

    @Value("${app.rag.vectorstore.load-mode:async}")
    private String loadMode;

    @Value("${app.rag.vectorstore.load.max-attempts:3}")
    private int maxAttempts;

    @Value("${app.rag.vectorstore.load.backoff-seconds:5}")
    private int backoffSeconds;

    private final AtomicBoolean loaded = new AtomicBoolean(false);
    private final AtomicBoolean retryTriggered = new AtomicBoolean(false);

    public LoveAppVectorStoreLoader(ApplicationContext applicationContext, LoveAppDocumentLoader documentLoader) {
        this.applicationContext = applicationContext;
        this.documentLoader = documentLoader;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if ("off".equalsIgnoreCase(loadMode)) {
            log.info("VectorStore document loading is disabled (app.rag.vectorstore.load-mode=off).");
            return;
        }
        loadDocumentsAsync();
    }

    /**
     * 异步加载文档，带有限重试。
     */
    CompletableFuture<Void> loadDocumentsAsync() {
        return CompletableFuture.runAsync(() -> loadWithRetry(1));
    }

    public void triggerRetryIfNotLoaded(String reason) {
        if ("off".equalsIgnoreCase(loadMode)) {
            log.info("VectorStore loading retry skipped because load-mode=off (reason={}).", reason);
            return;
        }

        if (loaded.get()) {
            return;
        }

        if (!retryTriggered.compareAndSet(false, true)) {
            return;
        }

        log.info("VectorStore loading retry triggered (reason={}).", reason);
        loadDocumentsAsync();
    }

    private void loadWithRetry(int attempt) {
        if (loaded.get()) {
            return;
        }

        try {
            log.info("VectorStore loading attempt {}/{}: loading documents...", attempt, maxAttempts);
            
            List<Document> documents = documentLoader.loadMarkdowns();
            if (documents.isEmpty()) {
                log.info("No documents found to load into VectorStore.");
                loaded.set(true);
                return;
            }

            VectorStore vectorStore = applicationContext.getBean("loveAppVectorStore", VectorStore.class);
            vectorStore.add(documents);
            loaded.set(true);
            log.info("Successfully loaded {} documents into VectorStore.", documents.size());

        } catch (Exception e) {
            log.warn("VectorStore loading attempt {}/{} failed: {}", attempt, maxAttempts, e.getMessage());

            if (attempt < maxAttempts) {
                long waitMs = (long) backoffSeconds * 1000 * (1L << (attempt - 1)); // exponential backoff
                log.info("Retrying VectorStore loading in {}ms...", waitMs);
                try {
                    Thread.sleep(waitMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("VectorStore loading retry interrupted.");
                    return;
                }
                loadWithRetry(attempt + 1);
            } else {
                log.error("VectorStore loading failed after {} attempts. Vector store remains empty.", maxAttempts);
            }
        }
    }

    // For testing
    boolean isLoaded() {
        return loaded.get();
    }
}
