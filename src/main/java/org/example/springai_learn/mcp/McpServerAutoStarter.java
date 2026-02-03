package org.example.springai_learn.mcp;

import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.rag.LoveAppVectorStoreLoader;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 在应用启动后自动构建 mcp-servers 模块。
 * 构建成功后仅输出日志提示；MCP client 需要用户通过环境变量 APP_MCP_CLIENT_ENABLED=true 启用。
 * 这保证了主程序启动不会因 MCP 不可用而阻塞或失败。
 */
@Component
@Slf4j
public class McpServerAutoStarter {

    private final boolean autostartEnabled;
    private final Environment environment;
    private final ApplicationContext applicationContext;
    private final McpBuildRunner buildRunner;
    private final LoveAppVectorStoreLoader vectorStoreLoader;

    public McpServerAutoStarter(
            Environment environment,
            ApplicationContext applicationContext,
            McpBuildRunner buildRunner,
            LoveAppVectorStoreLoader vectorStoreLoader
    ) {
        this.autostartEnabled = Boolean.parseBoolean(environment.getProperty("app.mcp.autostart", "true"));
        this.environment = environment;
        this.applicationContext = applicationContext;
        this.buildRunner = buildRunner;
        this.vectorStoreLoader = vectorStoreLoader;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        startAsync();
    }

    CompletableFuture<Void> startAsync() {
        if (!autostartEnabled) {
            log.info("MCP autostart disabled (app.mcp.autostart=false)");
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(this::buildMcpServers)
                .exceptionally(ex -> {
                    log.warn("MCP autostart failed (non-blocking): {}", ex.getMessage());
                    return null;
                });
    }

    private void buildMcpServers() {
        Path startDir = Paths.get(System.getProperty("user.dir"));
        Path repoRoot = McpAutostartSupport.findRepoRoot(startDir);
        if (repoRoot == null) {
            log.warn("Cannot locate repo root from user.dir='{}'; skip MCP autostart.", startDir.toAbsolutePath());
            return;
        }

        Path mcpServersDir = repoRoot.resolve("mcp-servers");
        if (!mcpServersDir.resolve("pom.xml").toFile().isFile()) {
            log.warn("Cannot locate mcp-servers Maven project at '{}'; skip MCP autostart.", mcpServersDir.toAbsolutePath());
            return;
        }

        List<String> buildCommand = McpAutostartSupport.buildMcpServersMavenPackageCommand();
        log.info("MCP autostart: building mcp-servers via: {} (cwd={})", String.join(" ", buildCommand), mcpServersDir);

        int exitCode = buildRunner.run(buildCommand, mcpServersDir, Duration.ofMinutes(10));
        if (exitCode != 0) {
            log.warn("MCP autostart: build failed with exitCode={}.", exitCode);
            return;
        }

        log.info("MCP autostart: build succeeded. Enabling MCP client (stdio) in background...");
        initializeMcpClient();
        triggerDocumentReloadIfNeeded("mcp-autostart");
    }

    private void initializeMcpClient() {
        try {
            enableMcpClientProperties();

            ObjectProvider<ToolCallbackProvider> provider = applicationContext.getBeanProvider(ToolCallbackProvider.class);
            ToolCallbackProvider callbackProvider = provider.getIfAvailable();
            if (callbackProvider != null) {
                log.info("MCP autostart: MCP client initialized via ToolCallbackProvider={}", callbackProvider.getClass().getSimpleName());
            } else {
                log.warn("MCP autostart: no ToolCallbackProvider bean available; ensure spring.ai.mcp.client.enabled=true");
            }
        } catch (Exception ex) {
            log.warn("MCP autostart: MCP client initialization failed (non-blocking): {}", ex.getMessage());
        }
    }

    private void enableMcpClientProperties() {
        setSystemPropertyIfAbsent("spring.ai.mcp.client.enabled", "true");
        setSystemPropertyIfAbsent("spring.ai.mcp.client.initialized", "true");
        log.info("MCP autostart: set spring.ai.mcp.client.enabled=true, spring.ai.mcp.client.initialized=true");
    }

    private void setSystemPropertyIfAbsent(String key, String value) {
        if (Boolean.parseBoolean(environment.getProperty(key, "false"))) {
            return;
        }

        if (environment instanceof ConfigurableEnvironment configurable) {
            configurable.getSystemProperties().put(key, value);
        } else {
            System.setProperty(key, value);
        }
    }

    private void triggerDocumentReloadIfNeeded(String reason) {
        if (vectorStoreLoader == null) {
            return;
        }

        vectorStoreLoader.triggerRetryIfNotLoaded(reason);
    }
}
