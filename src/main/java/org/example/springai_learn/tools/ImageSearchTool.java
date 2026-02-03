package org.example.springai_learn.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

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

    private static final String API_URL = "https://api.pexels.com/v1/search";

    public ImageSearchTool(String apiKey) {
        this(apiKey, 15000, 3);
    }

    public ImageSearchTool(String apiKey, int timeoutMs, int perPage) {
        this.apiKey = apiKey;
        this.timeoutMs = timeoutMs;
        this.perPage = perPage;
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
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", apiKey);

        Map<String, Object> params = new HashMap<>();
        params.put("query", query);
        params.put("page", 1);
        params.put("per_page", Math.max(1, Math.min(perPage, 10)));

        var httpResponse = HttpUtil.createGet(API_URL)
                .addHeaders(headers)
                .form(params)
                .timeout(Math.max(1000, timeoutMs))
                .execute();

        int status = httpResponse.getStatus();
        String responseBody = httpResponse.body();

        if (status < 200 || status >= 300) {
            throw new IllegalStateException("Pexels API error: status=" + status + ", body=" + responseBody);
        }

        return JSONUtil.parseObj(responseBody)
                .getJSONArray("photos")
                .stream()
                .map(photoObj -> (JSONObject) photoObj)
                .map(photoObj -> photoObj.getJSONObject("src"))
                .map(photo -> photo.getStr("medium"))
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }
}
