package org.example.springai_learn.mcp;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public interface McpBuildRunner {

    int run(List<String> command, Path workingDir, Duration timeout);
}
