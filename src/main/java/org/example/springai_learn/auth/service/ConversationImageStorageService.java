package org.example.springai_learn.auth.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.auth.entity.ConversationImage;
import org.example.springai_learn.auth.repository.ConversationImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

@Service
@Slf4j
public class ConversationImageStorageService {

    private final ConversationImageRepository conversationImageRepository;

    @Autowired(required = false)
    private SupabaseStorageClient supabaseStorageClient;

    @Value("${app.file-save-dir:}")
    private String configuredDir;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    public ConversationImageStorageService(ConversationImageRepository conversationImageRepository) {
        this.conversationImageRepository = conversationImageRepository;
    }

    public StoredConversationImage storeRemoteImage(String conversationId, String sourceUrl, String preferredFileName) {
        if (StrUtil.isBlank(conversationId)) {
            throw new IllegalArgumentException("conversationId 不能为空");
        }
        if (StrUtil.isBlank(sourceUrl)) {
            throw new IllegalArgumentException("sourceUrl 不能为空");
        }

        ConversationImage existing = conversationImageRepository
                .findFirstByConversationIdAndSourceUrl(conversationId, sourceUrl.trim())
                .orElse(null);
        if (existing != null) {
            return new StoredConversationImage(
                    existing.getFileName(),
                    existing.getPublicUrl(),
                    existing.getStoragePath(),
                    existing.getSourceUrl());
        }

        HttpResponse<byte[]> response = downloadImage(sourceUrl.trim());
        int status = response.statusCode();
        String contentType = response.headers().firstValue("Content-Type").orElse(null);
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("图片抓取失败: http status=" + status);
        }
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalStateException("图片抓取失败: unexpected content-type=" + contentType);
        }

        byte[] body = response.body();
        String fileName = buildFileName(preferredFileName, sourceUrl, contentType);
        StoredConversationImage storedImage = persistImage(conversationId, sourceUrl.trim(), fileName, body, contentType);

        ConversationImage image = ConversationImage.builder()
                .conversationId(conversationId)
                .fileName(storedImage.getFileName())
                .fileType("image")
                .publicUrl(storedImage.getPublicUrl())
                .sourceUrl(sourceUrl.trim())
                .storagePath(storedImage.getStoragePath())
                .build();
        conversationImageRepository.save(image);
        log.info("Coach 图片已持久化: chatId={}, fileName={}, storagePath={}, publicUrl={}",
                conversationId, storedImage.getFileName(), storedImage.getStoragePath(), storedImage.getPublicUrl());

        return storedImage;
    }

    private String buildFileName(String preferredFileName, String sourceUrl, String contentType) {
        String candidate = sanitizeFileName(preferredFileName);
        if (StrUtil.isBlank(candidate)) {
            candidate = sanitizeFileName(extractFileNameFromUrl(sourceUrl));
        }
        if (StrUtil.isBlank(candidate)) {
            candidate = "image-" + UUID.randomUUID();
        }

        String ext = FileUtil.extName(candidate);
        if (StrUtil.isBlank(ext)) {
            candidate = candidate + extensionFromContentType(contentType);
        }
        return candidate;
    }

    private String buildStoragePath(String conversationId, String fileName) {
        String safeFileName = sanitizeFileName(fileName);
        return "artifacts/" + conversationId + "/" + System.currentTimeMillis() + "-" + safeFileName;
    }

    private StoredConversationImage persistImage(String conversationId,
                                                 String sourceUrl,
                                                 String fileName,
                                                 byte[] body,
                                                 String contentType) {
        if (supabaseStorageClient != null && supabaseStorageClient.isConfigured()) {
            String storagePath = buildStoragePath(conversationId, fileName);
            try {
                String publicUrl = supabaseStorageClient.upload(body, storagePath, normalizeContentType(contentType));
                return new StoredConversationImage(fileName, publicUrl, storagePath, sourceUrl);
            } catch (Exception e) {
                log.warn("Supabase Storage 不可用，降级到本地文件: conversationId={}, fileName={}, error={}",
                        conversationId, fileName, e.getMessage());
            }
        }

        String storedFileName = resolveAvailableLocalFileName(fileName);
        Path downloadDir = resolveLocalDownloadDir();
        Path localFile = downloadDir.resolve(storedFileName).normalize();
        try {
            Files.createDirectories(downloadDir);
            Files.write(localFile, body);
        } catch (IOException e) {
            throw new IllegalStateException("保存图片到本地失败: " + localFile, e);
        }
        return new StoredConversationImage(
                storedFileName,
                "/api/files/download/" + storedFileName,
                "download/" + storedFileName,
                sourceUrl);
    }

    private HttpResponse<byte[]> downloadImage(String sourceUrl) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(sourceUrl))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "image/*")
                .header("User-Agent", "Lovemaster/1.0")
                .GET()
                .build();
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("图片抓取失败: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("图片抓取被中断", e);
        }
    }

    private Path resolveLocalDownloadDir() {
        String baseDir = StrUtil.isBlank(configuredDir)
                ? Path.of(System.getProperty("user.dir"), "tmp").toString()
                : configuredDir.trim();
        return Path.of(baseDir).toAbsolutePath().normalize().resolve("download");
    }

    private String resolveAvailableLocalFileName(String fileName) {
        String safeFileName = sanitizeFileName(fileName);
        Path downloadDir = resolveLocalDownloadDir();
        Path candidate = downloadDir.resolve(safeFileName).normalize();
        if (!Files.exists(candidate)) {
            return safeFileName;
        }

        String baseName = FileUtil.mainName(safeFileName);
        String ext = FileUtil.extName(safeFileName);
        return baseName + "_" + System.currentTimeMillis() + (StrUtil.isBlank(ext) ? "" : "." + ext);
    }

    private String sanitizeFileName(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return null;
        }
        String safeName = Path.of(fileName.trim()).getFileName().toString();
        return safeName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String extractFileNameFromUrl(String sourceUrl) {
        try {
            URI uri = new URI(sourceUrl.trim());
            String path = uri.getPath();
            if (StrUtil.isBlank(path)) {
                return null;
            }
            return Path.of(path).getFileName().toString();
        } catch (Exception e) {
            log.debug("从 sourceUrl 提取文件名失败: {}", sourceUrl);
            return null;
        }
    }

    private String extensionFromContentType(String contentType) {
        return switch (normalizeContentType(contentType)) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "image/jpeg";
        }
        String contentTypeValue = contentType;
        int commaIndex = contentTypeValue.indexOf(',');
        if (commaIndex >= 0) {
            contentTypeValue = contentTypeValue.substring(0, commaIndex);
        }
        int semicolonIndex = contentTypeValue.indexOf(';');
        String normalized = semicolonIndex >= 0 ? contentTypeValue.substring(0, semicolonIndex) : contentTypeValue;
        return normalized.trim().toLowerCase(Locale.ROOT);
    }

    @Getter
    @AllArgsConstructor
    public static class StoredConversationImage {
        private final String fileName;
        private final String publicUrl;
        private final String storagePath;
        private final String sourceUrl;
    }
}
