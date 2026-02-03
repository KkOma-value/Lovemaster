package org.example.springai_learn.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/files")
@CrossOrigin(originPatterns = "*", allowCredentials = "false")
public class FileController {

    @Value("${app.file-save-dir:}")
    private String configuredDir;

    private String getBaseDir() {
        if (configuredDir != null && !configuredDir.isBlank()) {
            return configuredDir.trim();
        }
        return System.getProperty("user.dir") + "/tmp";
    }

    @GetMapping("/{type}/{fileName}")
    public void getFile(@PathVariable String type, @PathVariable String fileName, HttpServletResponse response)
            throws IOException {
        log.info("FileController.getFile called: type={}, fileName={}", type, fileName);

        if (StrUtil.isBlank(type) || StrUtil.isBlank(fileName)) {
            log.warn("Bad request: type or fileName is blank");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // 安全检查：防止路径遍历
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            log.warn("Forbidden: path traversal detected in fileName: {}", fileName);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String baseDir = getBaseDir();
        log.info("Using baseDir: {}", baseDir);

        File folder;
        if ("pdf".equalsIgnoreCase(type)) {
            folder = new File(baseDir, "pdf");
        } else if ("download".equalsIgnoreCase(type)) {
            folder = new File(baseDir, "download");
        } else {
            log.warn("Bad request: unknown type: {}", type);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        File file = new File(folder, fileName);
        log.info("Looking for file: {} exists={}", file.getAbsolutePath(), file.exists());

        if (!file.exists() || !file.isFile()) {
            log.warn("File not found: {}", file.getAbsolutePath());
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 设置Content-Type
        String ext = FileUtil.extName(file).toLowerCase();
        String contentType = switch (ext) {
            case "pdf" -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            default -> "application/octet-stream";
        };
        response.setContentType(contentType);
        log.info("Serving file: {} with Content-Type: {}", file.getAbsolutePath(), contentType);

        // 写出文件
        FileUtil.writeToStream(file, response.getOutputStream());
    }
}
