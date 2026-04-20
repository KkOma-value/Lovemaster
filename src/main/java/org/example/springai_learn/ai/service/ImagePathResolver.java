package org.example.springai_learn.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.auth.service.ImageStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 把 imageUrl (http / 本地 API 路径) 解析为本地可读 {@link Path}。
 * 抽出自旧 {@code MultimodalIntakeService}，供 OcrAgentService 与 RewriteAgentService 共用。
 */
@Component
@Slf4j
public class ImagePathResolver {

    @Autowired(required = false)
    private ImageStorageService imageStorageService;

    private final RestTemplate restTemplate = new RestTemplate();

    public Path resolve(String userId, String imageUrl) throws Exception {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("图片 URL 为空");
        }

        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            log.info("检测到 public 图片 URL，开始下载到临时文件: {}", imageUrl);
            byte[] imageBytes = restTemplate.getForEntity(imageUrl, byte[].class).getBody();
            if (imageBytes == null) {
                throw new IllegalStateException("无法从 URL 下载图片: " + imageUrl);
            }
            String ext = guessExtensionFromUrl(imageUrl);
            Path tempFile = Files.createTempFile("lovemaster_img_", ext);
            Files.write(tempFile, imageBytes);
            log.info("图片已下载到临时文件: {}", tempFile);
            return tempFile;
        }

        if (imageStorageService == null) {
            throw new IllegalStateException("本地未启用图片存储服务，无法解析图片 URL");
        }

        String path = URI.create(imageUrl).getPath();
        String[] parts = path.split("/");
        if (parts.length < 5) {
            throw new IllegalArgumentException("无法解析图片 URL: " + imageUrl);
        }
        String parsedUserId = parts[parts.length - 3];
        String imageType = parts[parts.length - 2];
        String fileName = parts[parts.length - 1];
        if (userId != null && !userId.isBlank() && !"anonymous".equals(userId) && !userId.equals(parsedUserId)) {
            log.warn("图片 URL userId 与当前用户不一致，使用 URL 中的 userId 解析: current={}, url={}",
                    userId, parsedUserId);
        }
        return imageStorageService.getImagePath(parsedUserId, imageType, fileName);
    }

    public MimeType detectMimeType(Path imagePath) {
        try {
            String detected = Files.probeContentType(imagePath);
            if (detected != null && !detected.isBlank()) {
                return MimeTypeUtils.parseMimeType(detected);
            }
        } catch (Exception e) {
            log.debug("检测图片 MimeType 失败: {}", e.getMessage());
        }
        return MimeTypeUtils.IMAGE_PNG;
    }

    private String guessExtensionFromUrl(String imageUrl) {
        String lower = imageUrl.toLowerCase();
        if (lower.contains(".png")) return ".png";
        if (lower.contains(".jpg") || lower.contains(".jpeg")) return ".jpg";
        if (lower.contains(".webp")) return ".webp";
        if (lower.contains(".gif")) return ".gif";
        return ".tmp";
    }
}
