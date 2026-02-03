package org.example.springai_learn.tools;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceDownloadToolTest {

    private Path tempDir;
    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void setUp() throws Exception {
        // 创建临时目录作为保存路径
        tempDir = Files.createTempDirectory("resource-download-test-");

        // 启动Mock HTTP服务器
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        int port = server.getAddress().getPort();
        baseUrl = "http://localhost:" + port;
    }

    @AfterEach
    void tearDown() throws Exception {
        if (server != null) {
            server.stop(0);
        }
        // 清理临时文件
        if (tempDir != null) {
            Files.walk(tempDir)
                    .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (Exception ignored) {
                        }
                    });
        }
    }

    @Test
    void downloadResource_successfullyDownloadsImage() throws Exception {
        // 模拟图片响应
        server.createContext("/img.jpg", exchange -> {
            byte[] body = "fake-jpg-content".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "image/jpeg");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        ResourceDownloadTool tool = new ResourceDownloadTool(tempDir.toString());
        String fileName = "test.jpg";
        String url = baseUrl + "/img.jpg";

        String result = tool.downloadResource(url, fileName);

        // 验证结果
        assertTrue(result.contains("successfully"), "Result should indicate success: " + result);

        Path downloadedFile = tempDir.resolve("download").resolve(fileName);
        assertTrue(Files.exists(downloadedFile), "File should be downloaded");
        assertTrue(Files.size(downloadedFile) > 0, "File should not be empty");
    }

    @Test
    void downloadResource_rejectsHtmlContent() throws Exception {
        // 模拟HTML响应（如马蜂窝网页）
        server.createContext("/fake-img.jpg", exchange -> {
            byte[] body = "<html><body>Not an image</body></html>".getBytes(StandardCharsets.UTF_8);
            // 即使是200 OK，且URL以.jpg结尾，但Content-Type是text/html
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        ResourceDownloadTool tool = new ResourceDownloadTool(tempDir.toString());
        String fileName = "fake.jpg";
        String url = baseUrl + "/fake-img.jpg";

        String result = tool.downloadResource(url, fileName);

        // 验证结果
        assertTrue(result.contains("Error downloading resource"), "Result should indicate error: " + result);
        assertTrue(result.contains("unexpected content-type"), "Should complain about content type: " + result);
        assertTrue(result.contains("text/html"), "Should mention the actual content type: " + result);

        Path downloadedFile = tempDir.resolve("download").resolve(fileName);
        assertFalse(Files.exists(downloadedFile), "File should NOT be saved");
    }

    @Test
    void downloadResource_rejects404() throws Exception {
        // 模拟404
        server.createContext("/missing.jpg", exchange -> {
            exchange.sendResponseHeaders(404, -1);
        });

        ResourceDownloadTool tool = new ResourceDownloadTool(tempDir.toString());
        String fileName = "missing.jpg";
        String url = baseUrl + "/missing.jpg";

        String result = tool.downloadResource(url, fileName);

        // 验证结果
        assertTrue(result.contains("Error downloading resource"), "Result should indicate error: " + result);
        assertTrue(result.contains("http status=404"), "Should mention 404 status: " + result);

        Path downloadedFile = tempDir.resolve("download").resolve(fileName);
        assertFalse(Files.exists(downloadedFile), "File should NOT be saved");
    }
}
