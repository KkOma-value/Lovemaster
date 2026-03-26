package org.example.springai_learn.auth.controller;

import cn.hutool.core.io.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.auth.dto.ImageUploadResponse;
import org.example.springai_learn.auth.service.AuthService;
import org.example.springai_learn.auth.service.ImageStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/images")
@Slf4j
@RequiredArgsConstructor
public class ImageController {

    private final ImageStorageService imageStorageService;
    private final AuthService authService;

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam(defaultValue = "chat") String type,
                                    Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        try {
            ImageUploadResponse response = imageStorageService.store(file, userId, type);

            // If avatar upload, update user avatar
            if ("avatar".equals(type)) {
                authService.updateAvatar(userId, response.getUrl());
            }

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            log.error("图片上传失败: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "上传失败"));
        }
    }

    @GetMapping("/{userId}/{type}/{fileName}")
    public void getImage(@PathVariable String userId,
                         @PathVariable String type,
                         @PathVariable String fileName,
                         HttpServletResponse response) throws IOException {
        try {
            Path imagePath = imageStorageService.getImagePath(userId, type, fileName);
            File file = imagePath.toFile();

            if (!file.exists() || !file.isFile()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            String ext = FileUtil.extName(file).toLowerCase();
            String contentType = switch (ext) {
                case "jpg", "jpeg" -> "image/jpeg";
                case "png" -> "image/png";
                case "webp" -> "image/webp";
                default -> "application/octet-stream";
            };
            response.setContentType(contentType);
            FileUtil.writeToStream(file, response.getOutputStream());
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
    }
}
