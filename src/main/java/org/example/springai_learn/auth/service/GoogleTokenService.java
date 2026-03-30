package org.example.springai_learn.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@Slf4j
public class GoogleTokenService {

    @Value("${app.google.client-id:}")
    private String clientId;

    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    public void init() {
        if (clientId == null || clientId.isBlank()) {
            log.warn("Google Client ID 未配置，Google 登录不可用");
            return;
        }
        verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
        log.info("Google Token 验证器初始化完成，Client ID: {}...{}", clientId.substring(0, 8), clientId.substring(clientId.length() - 4));
    }

    public boolean isAvailable() {
        return verifier != null;
    }

    public GoogleUserInfo verify(String idTokenString) {
        if (verifier == null) {
            throw new IllegalStateException("Google 登录未配置");
        }
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new IllegalArgumentException("无效的 Google Token");
            }
            GoogleIdToken.Payload payload = idToken.getPayload();
            GoogleUserInfo info = new GoogleUserInfo();
            info.setGoogleId(payload.getSubject());
            info.setEmail(payload.getEmail());
            info.setName((String) payload.get("name"));
            info.setPictureUrl((String) payload.get("picture"));
            info.setEmailVerified(payload.getEmailVerified());
            return info;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google Token 验证失败", e);
            throw new IllegalArgumentException("Google Token 验证失败: " + e.getMessage());
        }
    }

    @Data
    public static class GoogleUserInfo {
        private String googleId;
        private String email;
        private String name;
        private String pictureUrl;
        private boolean emailVerified;
    }
}
