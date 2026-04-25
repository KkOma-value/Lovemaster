package org.example.springai_learn.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.config.KnowledgeProperties;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Wiki 内容变更 → graphify 知识图谱自动同步。
 *
 * 当 KnowledgeReinforcementJob 或 KnowledgeAutoApprovalJob 写入新的
 * wiki markdown 文件后，本服务负责异步调用 graphify 更新知识图谱，
 * 确保 graphify-out/ 始终反映最新 wiki 状态。
 *
 * 内置防抖: 30 秒内多次触发只执行最后一次。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WikiGraphSyncService {

    private final KnowledgeProperties properties;
    private final AtomicReference<Instant> lastTrigger = new AtomicReference<>(Instant.EPOCH);
    private final AtomicReference<CompletableFuture<Void>> pendingSync = new AtomicReference<>();

    /**
     * 请求同步 wiki → graph。带防抖，异步执行。
     *
     * @param triggerSource 触发来源描述，用于日志
     */
    public void requestSync(String triggerSource) {
        KnowledgeProperties.GraphSync config = properties.getGraphSync();
        if (!config.isEnabled()) {
            return;
        }

        Instant now = Instant.now();
        Instant last = lastTrigger.get();
        long sinceLastMs = Duration.between(last, now).toMillis();

        if (sinceLastMs < TimeUnit.SECONDS.toMillis(config.getDebounceSeconds())) {
            log.debug("GraphSync debounced: last trigger {}ms ago from [{}], source [{}]",
                    sinceLastMs, last, triggerSource);
            // 调度延迟执行
            scheduleDeferred(triggerSource, config);
            return;
        }

        lastTrigger.set(now);
        executeSync(triggerSource, config);
    }

    private void scheduleDeferred(String triggerSource, KnowledgeProperties.GraphSync config) {
        CompletableFuture<Void> existing = pendingSync.getAndSet(null);
        if (existing != null && !existing.isDone()) {
            existing.cancel(false);
        }

        CompletableFuture<Void> deferred = CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(config.getDebounceSeconds()));
                lastTrigger.set(Instant.now());
                executeSync(triggerSource + "-deferred", config);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        pendingSync.set(deferred);
    }

    private void executeSync(String triggerSource, KnowledgeProperties.GraphSync config) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("GraphSync starting, trigger=[{}]", triggerSource);

                String command = config.getGraphifyCommand();
                String outputDir = config.getOutputDir();

                ProcessBuilder pb = new ProcessBuilder(command, "update", ".");
                pb.directory(Path.of(".").toFile());

                Process process = pb.start();
                boolean finished = process.waitFor(120, TimeUnit.SECONDS);

                if (finished) {
                    int exitCode = process.exitValue();
                    if (exitCode == 0) {
                        log.info("GraphSync completed successfully, trigger=[{}]", triggerSource);
                    } else {
                        String stderr = new BufferedReader(
                                new InputStreamReader(process.getErrorStream()))
                                .lines()
                                .collect(java.util.stream.Collectors.joining("\n"));
                        log.warn("GraphSync failed with exitCode={}, trigger=[{}], stderr={}",
                                exitCode, triggerSource, stderr);
                    }
                } else {
                    process.destroyForcibly();
                    log.warn("GraphSync timed out, trigger=[{}]", triggerSource);
                }
            } catch (Exception ex) {
                log.warn("GraphSync execution failed, trigger=[{}], error={}",
                        triggerSource, ex.getMessage());
            }
        });
    }
}
