package org.example.springai_learn.mcp;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class McpAutostartSupport {

    private McpAutostartSupport() {
    }

    static Path findRepoRoot(Path start) {
        if (start == null) {
            return null;
        }

        Path current = start.toAbsolutePath().normalize();
        for (int i = 0; i < 10 && current != null; i++) {
            if (Files.isRegularFile(current.resolve("pom.xml"))
                    && Files.isDirectory(current.resolve("mcp-servers"))
                    && Files.isRegularFile(current.resolve("mcp-servers").resolve("pom.xml"))) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    static List<String> buildMcpServersMavenPackageCommand() {
        // Use mvn.cmd on Windows when available; fall back to mvn.
        String mvn = isWindows() ? "mvn.cmd" : "mvn";

        List<String> command = new ArrayList<>();
        command.add(mvn);
        command.add("-DskipTests");
        command.add("package");
        return command;
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().contains("win");
    }
}
