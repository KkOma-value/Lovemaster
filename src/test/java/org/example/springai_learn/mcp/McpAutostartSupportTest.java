package org.example.springai_learn.mcp;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class McpAutostartSupportTest {

    @Test
    void findRepoRoot_fromUserDir_shouldFindPom() {
        Path userDir = Paths.get(System.getProperty("user.dir"));
        Path root = McpAutostartSupport.findRepoRoot(userDir);
        assertNotNull(root, "Should locate repo root (pom.xml + mcp-servers/pom.xml)");
        assertTrue(root.resolve("pom.xml").toFile().isFile());
        assertTrue(root.resolve("mcp-servers").resolve("pom.xml").toFile().isFile());
    }

    @Test
    void buildMcpServersMavenPackageCommand_shouldContainModuleAndSkipTests() {
        List<String> cmd = McpAutostartSupport.buildMcpServersMavenPackageCommand();
        String joined = String.join(" ", cmd);
        assertTrue(joined.contains("-DskipTests"));
        assertTrue(joined.contains("package"));
    }
}
