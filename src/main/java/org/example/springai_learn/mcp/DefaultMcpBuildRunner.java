package org.example.springai_learn.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class DefaultMcpBuildRunner implements McpBuildRunner {

    @Override
    public int run(List<String> command, Path workingDir, Duration timeout) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(workingDir.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null) {
                    if (lineCount < 200) {
                        log.info("[mcp-autostart][build] {}", line);
                    } else if (lineCount == 200) {
                        log.info("[mcp-autostart][build] ... (output truncated)");
                    }
                    lineCount++;
                }
            }

            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("MCP autostart: build timed out after {}", timeout);
                return 124;
            }

            return process.exitValue();
        } catch (Exception e) {
            log.warn("MCP autostart: failed to run build command: {}", e.getMessage());
            return 127;
        }
    }
}
