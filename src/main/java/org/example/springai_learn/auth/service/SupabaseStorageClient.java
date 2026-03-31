package org.example.springai_learn.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

/**
 * Supabase Storage 轻量 REST 客户端
 * 负责上传文件到 Supabase Storage 并生成 public URL
 */
@Component
@Slf4j
public class SupabaseStorageClient {

    @Value("${supabase.url:}")
    private String supabaseUrl;

    @Value("${supabase.service-role-key:}")
    private String serviceRoleKey;

    @Value("${supabase.storage.bucket:conversation-images}")
    private String bucket;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 上传文件到 Supabase Storage
     *
     * @param data        文件字节数组
     * @param storagePath 存储路径（bucket 内相对路径）
     * @param contentType MIME 类型，如 image/png
     * @return 拼接好的 public URL
     */
    public String upload(byte[] data, String storagePath, String contentType) {
        if (supabaseUrl == null || supabaseUrl.isBlank() || serviceRoleKey == null || serviceRoleKey.isBlank()) {
            throw new IllegalStateException("Supabase URL 或 Service Role Key 未配置");
        }

        String url = supabaseUrl + "/storage/v1/object/" + bucket + "/" + sanitizePath(storagePath);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(serviceRoleKey);
        headers.set("apikey", serviceRoleKey);
        headers.set("x-upsert", "true");
        headers.setContentType(MediaType.parseMediaType(contentType));

        HttpEntity<byte[]> request = new HttpEntity<>(data, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, request, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            log.info("Supabase Storage 上传成功: path={}", storagePath);
            return getPublicUrl(storagePath);
        }
        throw new RuntimeException("Supabase Storage 上传失败: " + response.getStatusCode());
    }

    /**
     * 获取文件的 public URL（要求 bucket 为 public）
     */
    public String getPublicUrl(String storagePath) {
        return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + sanitizePath(storagePath);
    }

    public boolean isConfigured() {
        return supabaseUrl != null && !supabaseUrl.isBlank()
                && serviceRoleKey != null && !serviceRoleKey.isBlank();
    }

    private String sanitizePath(String path) {
        return path.replace("\\", "/").replaceAll("/+", "/");
    }
}
