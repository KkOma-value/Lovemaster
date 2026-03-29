package org.example.springai_learn.auth.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.auth.dto.AuthResponse;
import org.example.springai_learn.auth.dto.LoginRequest;
import org.example.springai_learn.auth.dto.RegisterRequest;
import org.example.springai_learn.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    @Autowired
    private AuthService authService;

    private ResponseEntity<?> checkAvailable() {
        if (!authService.isAvailable()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "认证服务不可用（数据库未连接）"));
        }
        return null;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        ResponseEntity<?> check = checkAvailable();
        if (check != null) return check;
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        ResponseEntity<?> check = checkAvailable();
        if (check != null) return check;
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(Authentication authentication) {
        ResponseEntity<?> check = checkAvailable();
        if (check != null) return check;
        String userId = (String) authentication.getPrincipal();
        authService.logout(userId);
        return ResponseEntity.ok(Map.of("message", "已登出"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {
        ResponseEntity<?> check = checkAvailable();
        if (check != null) return check;
        try {
            String refreshToken = request.get("refreshToken");
            AuthResponse response = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        ResponseEntity<?> check = checkAvailable();
        if (check != null) return check;
        String userId = (String) authentication.getPrincipal();
        AuthResponse.UserInfo userInfo = authService.getCurrentUser(userId);
        return ResponseEntity.ok(userInfo);
    }
}
