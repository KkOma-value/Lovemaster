package org.example.springai_learn.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.auth.dto.ImageUploadResponse;
import org.example.springai_learn.auth.entity.UserImage;
import org.example.springai_learn.auth.repository.UserImageRepository;
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
@RequiredArgsConstructor
@Slf4j
public class ImageStorageService {

    private final UserImageRepository userImageRepository;

    @Value("${app.file-save-dir:${user.dir}/tmp}")
    private String baseSaveDir;

    @Value("${app.image.max-size:5242880}")
    private long maxSize;

    @Value("${app.image.allowed-types:image/jpeg,image/png,image/webp}")
    private String allowedTypes;

    public ImageUploadResponse store(MultipartFile file, String userId, String imageType) throws IOException {
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

        // Create directory
        Path uploadDir = Paths.get(baseSaveDir, "images", userId, imageType);
        Files.createDirectories(uploadDir);

        // Save file
        Path filePath = uploadDir.resolve(fileName);
        file.transferTo(filePath.toFile());
        log.info("图片已保存: path={}, size={}", filePath, file.getSize());

        // Save to database
        UserImage userImage = UserImage.builder()
                .userId(userId)
                .fileName(fileName)
                .originalName(originalName)
                .fileSize(file.getSize())
                .contentType(contentType)
                .imageType(imageType)
                .build();
        userImageRepository.save(userImage);

        // Build access URL
        String url = "/api/images/" + userId + "/" + imageType + "/" + fileName;

        return ImageUploadResponse.builder()
                .id(userImage.getId())
                .url(url)
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
