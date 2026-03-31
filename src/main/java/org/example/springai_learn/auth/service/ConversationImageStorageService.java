package org.example.springai_learn.auth.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.auth.entity.ConversationImage;
import org.example.springai_learn.auth.repository.ConversationImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

@Service
@Slf4j
public class ConversationImageStorageService {

    private final ConversationImageRepository conversationImageRepository;

    @Autowired(required = false)
    private SupabaseStorageClient supabaseStorageClient;

    public ConversationImageStorageService(ConversationImageRepository conversationImageRepository) {
        this.conversationImageRepository = conversationImageRepository;
    }

    public StoredConversationImage storeRemoteImage(String conversationId, String sourceUrl, String preferredFileName) {
        if (supabaseStorageClient == null) {
            throw new IllegalStateException("Supabase Storage 未配置，无法持久化 Coach 图片");
        }
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

        var response = HttpUtil.createGet(sourceUrl.trim())
                .timeout(30000)
                .execute();

        int status = response.getStatus();
        String contentType = response.header("Content-Type");
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("图片抓取失败: http status=" + status);
        }
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalStateException("图片抓取失败: unexpected content-type=" + contentType);
        }

        byte[] body = response.bodyBytes();
        String fileName = buildFileName(preferredFileName, sourceUrl, contentType);
        String storagePath = buildStoragePath(conversationId, fileName);
        String publicUrl = supabaseStorageClient.upload(body, storagePath, normalizeContentType(contentType));

        ConversationImage image = ConversationImage.builder()
                .conversationId(conversationId)
                .fileName(fileName)
                .fileType("image")
                .publicUrl(publicUrl)
                .sourceUrl(sourceUrl.trim())
                .storagePath(storagePath)
                .build();
        conversationImageRepository.save(image);
        log.info("Coach 图片已保存到 Supabase: chatId={}, fileName={}, storagePath={}",
                conversationId, fileName, storagePath);

        return new StoredConversationImage(fileName, publicUrl, storagePath, sourceUrl.trim());
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
        int semicolonIndex = contentType.indexOf(';');
        String normalized = semicolonIndex >= 0 ? contentType.substring(0, semicolonIndex) : contentType;
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
