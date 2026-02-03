package org.example.springai_learn.mcp;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.env.MockEnvironment;
import org.example.springai_learn.rag.LoveAppVectorStoreLoader;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class McpServerAutoStarterTest {

    @Test
    void startAsync_whenDisabled_shouldNotRunBuild() {
        MockEnvironment env = new MockEnvironment().withProperty("app.mcp.autostart", "false");
        ApplicationContext ctx = Mockito.mock(ApplicationContext.class);
        LoveAppVectorStoreLoader loader = Mockito.mock(LoveAppVectorStoreLoader.class);

        AtomicInteger calls = new AtomicInteger();
        McpBuildRunner runner = (command, workingDir, timeout) -> {
            calls.incrementAndGet();
            return 0;
        };

        McpServerAutoStarter starter = new McpServerAutoStarter(env, ctx, runner, loader);
        starter.startAsync().join();

        assertEquals(0, calls.get());
        Mockito.verifyNoInteractions(loader);
    }

    @Test
    void startAsync_whenEnabledAndBuildSucceeds_shouldComplete() {
        MockEnvironment env = new MockEnvironment().withProperty("app.mcp.autostart", "true");
        ApplicationContext ctx = Mockito.mock(ApplicationContext.class);
        ObjectProvider<ToolCallbackProvider> provider = Mockito.mock(ObjectProvider.class);
        Mockito.when(ctx.getBeanProvider(ToolCallbackProvider.class)).thenReturn(provider);
        Mockito.when(provider.getIfAvailable()).thenReturn(null);
        LoveAppVectorStoreLoader loader = Mockito.mock(LoveAppVectorStoreLoader.class);

        AtomicInteger calls = new AtomicInteger();
        McpBuildRunner runner = (List<String> command, Path workingDir, Duration timeout) -> {
            calls.incrementAndGet();
            return 0;
        };

        McpServerAutoStarter starter = new McpServerAutoStarter(env, ctx, runner, loader);
        starter.startAsync().join();

        assertEquals(1, calls.get());
        Mockito.verify(loader, Mockito.times(1)).triggerRetryIfNotLoaded("mcp-autostart");
    }
}
