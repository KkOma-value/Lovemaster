package org.example.springai_learn.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 图片搜索工具，使用 Pexels API 搜索直接图片URL
 * 返回的URL可以直接用于 downloadResource 工具下载
 */
@Slf4j
public class ImageSearchTool {

    private final String apiKey;
    private final int timeoutMs;
    private final int perPage;
    private final String apiUrl;
    private final HttpClient httpClient;
    private final int maxRetries;

    private static final String API_URL = "https://api.pexels.com/v1/search";
    private static final String USER_AGENT = "Lovemaster/1.0";

    public ImageSearchTool(String apiKey) {
        this(apiKey, 15000, 3);
    }

    public ImageSearchTool(String apiKey, int timeoutMs, int perPage) {
        this(apiKey, timeoutMs, perPage, API_URL, defaultHttpClient(timeoutMs), 2);
    }

    ImageSearchTool(String apiKey, int timeoutMs, int perPage, String apiUrl, HttpClient httpClient, int maxRetries) {
        this.apiKey = apiKey;
        this.timeoutMs = timeoutMs;
        this.perPage = perPage;
        this.apiUrl = apiUrl;
        this.httpClient = httpClient;
        this.maxRetries = Math.max(0, maxRetries);
    }

    @Tool(description = "Search for images on Pexels and return DIRECT image URLs that can be downloaded. Use this tool to find real image URLs before calling downloadResource.")
    public String searchImage(
            @ToolParam(description = "Search keyword (e.g., 'West Lake', 'Beijing', 'cat')") String query) {
        if (StrUtil.isBlank(apiKey)) {
            return "Error: Pexels API key is not configured. Please set pexels.api-key in application config.";
        }
        if (StrUtil.isBlank(query)) {
            return "Error: Search query cannot be empty.";
        }

        try {
            List<String> urls = searchMediumImages(query);
            if (urls.isEmpty()) {
                return "No images found for query: " + query;
            }
            // 返回结果，包含使用说明
            StringBuilder result = new StringBuilder();
            result.append("Found ").append(urls.size()).append(" images for '").append(query).append("':\n");
            for (int i = 0; i < urls.size(); i++) {
                result.append(i + 1).append(". ").append(urls.get(i)).append("\n");
            }
            result.append(
                    "\nUse downloadResource with one of these URLs and a filename like 'image1.jpg' to download.");
            return result.toString();
        } catch (Exception e) {
            log.error("Error searching images", e);
            return "Error searching images: " + e.getMessage();
        }
    }

    /**
     * 搜索中等尺寸的图片列表
     */
    private List<String> searchMediumImages(String query) {
        Map<String, Object> params = new HashMap<>();
        params.put("query", query);
        params.put("page", 1);
        params.put("per_page", Math.max(1, Math.min(perPage, 10)));

        URI uri = buildSearchUri(params);
        String responseBody = executeSearchRequest(uri);

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
        return URI.create(apiUrl + "?" + queryString);
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
        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int status = response.statusCode();
                if (status < 200 || status >= 300) {
                    throw new IllegalStateException("Pexels API error: status=" + status + ", body=" + response.body());
                }
                return response.body();
            } catch (IOException e) {
                lastException = e;
                if (attempt > maxRetries) {
                    break;
                }
                log.warn("Pexels API 请求失败，准备重试: attempt={}, error={}", attempt, e.getMessage());
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

    private static HttpClient defaultHttpClient(int timeoutMs) {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(1000, timeoutMs)))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
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
