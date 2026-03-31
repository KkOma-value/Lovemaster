package org.example.springai_learn.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.auth.dto.ImageUploadResponse;
import org.example.springai_learn.auth.entity.ConversationImage;
import org.example.springai_learn.auth.entity.UserImage;
import org.example.springai_learn.auth.repository.ConversationImageRepository;
import org.example.springai_learn.auth.repository.UserImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ImageStorageService {

    @Autowired(required = false)
    private UserImageRepository userImageRepository;

    @Autowired(required = false)
    private ConversationImageRepository conversationImageRepository;

    @Autowired(required = false)
    private SupabaseStorageClient supabaseStorageClient;

    @Value("${app.file-save-dir:${user.dir}/tmp}")
    private String baseSaveDir;

    @Value("${app.image.max-size:5242880}")
    private long maxSize;

    @Value("${app.image.allowed-types:image/jpeg,image/png,image/webp}")
    private String allowedTypes;

    public boolean isAvailable() {
        return supabaseStorageClient != null;
    }

    public ImageUploadResponse store(MultipartFile file, String userId, String imageType) throws IOException {
        return store(file, userId, imageType, null);
    }

    public ImageUploadResponse store(MultipartFile file, String userId, String imageType, String conversationId) throws IOException {
        // Validate
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("文件大小超过限制 (最大 " + (maxSize / 1024 / 1024) + "MB)");
        }
        String contentType = file.getContentType();
        if (contentType == null || !List.of(allowedTypes.split(",")).contains(contentType)) {
            throw new IllegalArgumentException("不支持的文件类型: " + contentType);
        }

        // Generate unique filename
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID() + ext;

        // Build storage path for Supabase
        String storagePath = "images/" + userId + "/" + imageType + "/" + fileName;

        // Also save locally as cache / fallback
        Path uploadDir = Paths.get(baseSaveDir, "images", userId, imageType);
        Files.createDirectories(uploadDir);
        Path localFilePath = uploadDir.resolve(fileName);
        file.transferTo(localFilePath.toFile());
        log.info("图片已缓存到本地: path={}, size={}", localFilePath, file.getSize());

        // Upload to Supabase Storage
        String publicUrl;
        if (supabaseStorageClient != null) {
            publicUrl = supabaseStorageClient.upload(file.getBytes(), storagePath, contentType);
        } else {
            log.warn("Supabase Storage 未配置，回退到本地 URL");
            publicUrl = "/api/images/" + userId + "/" + imageType + "/" + fileName;
        }

        // Save metadata to database
        String imageId = null;
        if (conversationId != null && conversationId.isBlank()) {
            conversationId = null;
        }

        if (conversationId != null && conversationImageRepository != null) {
            ConversationImage conversationImage = ConversationImage.builder()
                    .conversationId(conversationId)
                    .fileName(fileName)
                    .fileType(contentType)
                    .publicUrl(publicUrl)
                    .storagePath(storagePath)
                    .build();
            conversationImageRepository.save(conversationImage);
            imageId = conversationImage.getId();
        } else if (userImageRepository != null) {
            UserImage userImage = UserImage.builder()
                    .userId(userId)
                    .fileName(fileName)
                    .originalName(originalName)
                    .fileSize(file.getSize())
                    .contentType(contentType)
                    .imageType(imageType)
                    .build();
            userImageRepository.save(userImage);
            imageId = userImage.getId();
        }

        return ImageUploadResponse.builder()
                .id(imageId)
                .url(publicUrl)
                .fileName(fileName)
                .fileSize(file.getSize())
                .build();
    }

    public Path getImagePath(String userId, String imageType, String fileName) {
        // Security: prevent path traversal
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new IllegalArgumentException("非法文件名");
        }
        return Paths.get(baseSaveDir, "images", userId, imageType, fileName);
    }
}
