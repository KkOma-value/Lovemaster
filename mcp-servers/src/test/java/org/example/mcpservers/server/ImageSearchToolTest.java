package org.example.mcpservers.server;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ImageSearchToolTest {

    @Test
    void downloadImage_writesIntoConfiguredFileSaveDir() throws Exception {
        Path baseDir = Files.createTempDirectory("mcp-file-save-").toAbsolutePath().normalize();
        Path expectedDownloadDir = baseDir.resolve("download");

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/img.jpg", exchange -> {
            byte[] body = "fake-jpg".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "image/jpeg");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();

        int port = server.getAddress().getPort();
        String url = "http://localhost:" + port + "/img.jpg";

        ImageSearchTool tool = new ImageSearchTool();
        // Inject config without Spring: set field via reflection to keep test lightweight.
        var field = ImageSearchTool.class.getDeclaredField("fileSaveDir");
        field.setAccessible(true);
        field.set(tool, baseDir.toString());

        String fileName = "gugong.jpg";
        try {
            String result = tool.downloadImage(url, fileName);

            assertTrue(result.startsWith("Image downloaded successfully"), result);
            assertTrue(result.contains(expectedDownloadDir.toString()), result);

            Path expectedFile = expectedDownloadDir.resolve(fileName);
            assertTrue(Files.exists(expectedFile), "downloaded file should exist: " + expectedFile);
            assertTrue(Files.size(expectedFile) > 0, "downloaded file should be non-empty");
        } finally {
            server.stop(0);
            // Cleanup
            Files.walk(baseDir)
                    .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (Exception ignored) {
                        }
                    });
        }
    }
}
