package org.example.springai_learn.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.auth.dto.AuthResponse;
import org.example.springai_learn.auth.dto.LoginRequest;
import org.example.springai_learn.auth.dto.RegisterRequest;
import org.example.springai_learn.auth.entity.RefreshToken;
import org.example.springai_learn.auth.entity.User;
import org.example.springai_learn.auth.repository.RefreshTokenRepository;
import org.example.springai_learn.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class AuthService {

    @Autowired(required = false)
    private UserRepository userRepository;

    @Autowired(required = false)
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public boolean isAvailable() {
        return userRepository != null;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Validate input
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("昵称不能为空");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("邮箱不能为空");
        }
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new IllegalArgumentException("密码至少8位");
        }

        // Check duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("该邮箱已注册");
        }

        // Create user
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();
        userRepository.save(user);
        log.info("新用户注册: id={}, email={}", user.getId(), user.getEmail());

        return generateAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("邮箱或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("邮箱或密码错误");
        }

        log.info("用户登录: id={}, email={}", user.getId(), user.getEmail());
        return generateAuthResponse(user);
    }

    @Transactional
    public void logout(String userId) {
        refreshTokenRepository.deleteByUserId(userId);
        log.info("用户登出: id={}", userId);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenStr) {
        if (!jwtService.validateToken(refreshTokenStr)) {
            throw new IllegalArgumentException("无效的刷新令牌");
        }

        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new IllegalArgumentException("刷新令牌不存在"));

        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(storedToken);
            throw new IllegalArgumentException("刷新令牌已过期");
        }

        String userId = jwtService.extractUserId(refreshTokenStr);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        // Delete old refresh token
        refreshTokenRepository.delete(storedToken);

        log.info("刷新令牌: userId={}", userId);
        return generateAuthResponse(user);
    }

    public AuthResponse.UserInfo getCurrentUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return AuthResponse.UserInfo.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    @Transactional
    public AuthResponse.UserInfo updateAvatar(String userId, String avatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);
        return AuthResponse.UserInfo.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getName());
        String refreshTokenStr = jwtService.generateRefreshToken(user.getId());

        // Store refresh token
        long refreshExpMs = 604800000L; // 7 days — should match config
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .token(refreshTokenStr)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpMs / 1000))
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .avatarUrl(user.getAvatarUrl())
                        .build())
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .build();
    }
}
