package org.example.mcpservers.server;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    // Pexels 常规搜索接口（请以文档为准）
    private static final String API_URL = "https://api.pexels.com/v1/search";

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
            return "Image downloaded successfully to: " + target +
                    " (status=" + status + ", contentType=" + contentType +
                    ", bytes=" + savedSize + ", downloadDir=" + downloadDir + ")";
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
        // 设置请求头（包含API密钥）
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", apiKey);

        // 设置请求参数（可按文档补充 page、per_page 等参数）
        Map<String, Object> params = new HashMap<>();
        params.put("query", query);
        params.put("page", 1);
        params.put("per_page", Math.max(1, Math.min(perPage, 10)));

        // 发送 GET 请求（设置超时，避免 MCP callTool 默认 20s 等待超时）
        var httpResponse = HttpUtil.createGet(API_URL)
                .addHeaders(headers)
                .form(params)
            .timeout(Math.max(1000, timeoutMs))
                .execute()
            ;

        int status = httpResponse.getStatus();
        String responseBody = httpResponse.body();
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("Pexels API error: status=" + status + ", body=" + responseBody);
        }

        // 解析响应JSON（假设响应结构包含"photos"数组，每个元素包含"medium"字段）
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
