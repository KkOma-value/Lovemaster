package org.example.mcpservers.server;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ImageSearchTool {

    @Value("${app.file-save-dir:}")
    private String fileSaveDir;

    @Value("${pexels.api-key:${PEXELS_API_KEY:}}")
    private String apiKey;

    @Value("${pexels.timeout-ms:15000}")
    private int timeoutMs;

    @Value("${pexels.per-page:3}")
    private int perPage;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    // Pexels 常规搜索接口（请以文档为准）
    private static final String API_URL = "https://api.pexels.com/v1/search";
    private static final String USER_AGENT = "Lovemaster-MCP/1.0";

    @Tool(description = "search image from web")
    public String searchImage(@ToolParam(description = "Search query keyword") String query) {
        try {
            return String.join(",", searchMediumImages(query));
        } catch (Exception e) {
            return "Error search image: " + e.getMessage();
        }
    }

    @Tool(description = "download image from url and save to tmp/download/<fileName>")
    public String downloadImage(
            @ToolParam(description = "Image URL") String url,
            @ToolParam(description = "File name to save (e.g., gugong.jpg)") String fileName) {
        if (StrUtil.isBlank(url)) {
            return "Error download image: url is blank";
        }
        if (StrUtil.isBlank(fileName)) {
            return "Error download image: fileName is blank";
        }
        String safeName = Path.of(fileName.trim()).getFileName().toString();
        Path baseDir = resolveFileSaveDir();
        Path downloadDir = baseDir.resolve("download");
        Path target = downloadDir.resolve(safeName).normalize();
        try {
            Files.createDirectories(downloadDir);
            var response = HttpUtil.createGet(url)
                    .timeout(Math.max(1000, timeoutMs))
                    .execute();

            int status = response.getStatus();
            String contentType = response.header("Content-Type");
            if (status < 200 || status >= 300) {
                return "Error download image: http status=" + status + " contentType=" + contentType;
            }
            if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                return "Error download image: unexpected content-type=" + contentType + " status=" + status;
            }

            byte[] body = response.bodyBytes();
            Files.write(target, body);
            long savedSize = Files.size(target);
            return "Image downloaded successfully\n"
                    + "sourceUrl: " + url + "\n"
                    + "fileName: " + safeName + "\n"
                    + "contentType: " + contentType + "\n"
                    + "bytes: " + savedSize + "\n"
                    + "localPath: " + target + "\n"
                    + "downloadDir: " + downloadDir + "\n"
                    + "status: " + status;
        } catch (Exception e) {
            return "Error download image: " + e.getMessage();
        }
    }

    private Path resolveFileSaveDir() {
        if (StrUtil.isBlank(fileSaveDir)) {
            return Path.of(System.getProperty("user.dir"), "tmp").toAbsolutePath().normalize();
        }
        return Path.of(fileSaveDir.trim()).toAbsolutePath().normalize();
    }

    /**
     * 搜索中等尺寸的图片列表
     *
     * @param query
     * @return
     */
    public List<String> searchMediumImages(String query) {
        if (StrUtil.isBlank(apiKey)) {
            throw new IllegalStateException("Pexels API key is not configured (set PEXELS_API_KEY or pexels.api-key)");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("query", query);
        params.put("page", 1);
        params.put("per_page", Math.max(1, Math.min(perPage, 10)));

        String responseBody = executeSearchRequest(buildSearchUri(params));

        return JSONUtil.parseObj(responseBody)
                .getJSONArray("photos")
                .stream()
                .map(photoObj -> (JSONObject) photoObj)
                .map(photoObj -> photoObj.getJSONObject("src"))
                .map(photo -> photo.getStr("medium"))
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }

    private URI buildSearchUri(Map<String, Object> params) {
        String queryString = params.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(String.valueOf(entry.getValue())))
                .collect(Collectors.joining("&"));
        return URI.create(API_URL + "?" + queryString);
    }

    private String executeSearchRequest(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMillis(Math.max(1000, timeoutMs)))
                .header("Authorization", apiKey)
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();

        Exception lastException = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int status = response.statusCode();
                if (status < 200 || status >= 300) {
                    throw new IllegalStateException("Pexels API error: status=" + status + ", body=" + response.body());
                }
                return response.body();
            } catch (IOException e) {
                lastException = e;
                if (attempt >= 3) {
                    break;
                }
                sleepBeforeRetry(attempt);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Pexels API request interrupted", e);
            }
        }
        throw new IllegalStateException("Pexels API request failed after retries: " + rootMessage(lastException), lastException);
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(Math.min(250L * attempt, 1000L));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry interrupted", e);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null) {
            current = current.getCause();
        }
        return current == null ? "unknown error" : current.getMessage();
    }
}
