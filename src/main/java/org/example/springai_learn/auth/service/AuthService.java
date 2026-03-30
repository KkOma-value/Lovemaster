package org.example.springai_learn.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.auth.dto.AuthResponse;
import org.example.springai_learn.auth.dto.LoginRequest;
import org.example.springai_learn.auth.dto.RegisterRequest;
import org.example.springai_learn.auth.entity.RefreshToken;
import org.example.springai_learn.auth.entity.User;
import org.example.springai_learn.auth.repository.RefreshTokenRepository;
import org.example.springai_learn.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username:}")
    private String mailUsername;

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

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new IllegalArgumentException("该账号通过 Google 注册，请使用 Google 登录或先设置密码");
        }

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

    @Transactional
    public AuthResponse googleLogin(String email, String name, String googleId, String pictureUrl) {
        Optional<User> existingUser = userRepository.findByEmail(email);
        boolean isNewUser;
        User user;

        if (existingUser.isPresent()) {
            // Existing user — merge Google account
            user = existingUser.get();
            if (user.getGoogleId() == null) {
                user.setGoogleId(googleId);
                if (user.getAvatarUrl() == null && pictureUrl != null) {
                    user.setAvatarUrl(pictureUrl);
                }
                userRepository.save(user);
            }
            isNewUser = false;
            log.info("Google 登录（已有用户）: id={}, email={}", user.getId(), email);
        } else {
            // New user — create without password
            user = User.builder()
                    .name(name != null ? name : email.split("@")[0])
                    .email(email)
                    .googleId(googleId)
                    .authProvider("google")
                    .needsPassword(true)
                    .avatarUrl(pictureUrl)
                    .build();
            userRepository.save(user);
            isNewUser = true;
            log.info("Google 注册新用户: id={}, email={}", user.getId(), email);

            // Send welcome email asynchronously
            sendWelcomeEmail(email, user.getName());
        }

        AuthResponse response = generateAuthResponse(user);
        response.setNeedsPassword(user.isNeedsPassword());
        response.setNewUser(isNewUser);
        return response;
    }

    @Transactional
    public void setPassword(String userId, String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("密码至少8位");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNeedsPassword(false);
        userRepository.save(user);
        log.info("用户设置密码: id={}", userId);
    }

    private void sendWelcomeEmail(String toEmail, String userName) {
        if (mailSender == null) {
            log.warn("邮件服务不可用，跳过发送欢迎邮件到 {}", toEmail);
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                message.setFrom(new InternetAddress(mailUsername, "LoveMaster"));
                message.setRecipients(MimeMessage.RecipientType.TO, toEmail);
                message.setSubject("Welcome to LoveMaster!");
                message.setText(
                        "Hi " + userName + ",\n\n" +
                        "Welcome to LoveMaster! Your account has been created successfully via Google.\n\n" +
                        "Please set a local password to secure your account.\n\n" +
                        "Best regards,\nThe LoveMaster Team",
                        "UTF-8"
                );
                mailSender.send(message);
                log.info("欢迎邮件已发送至 {}", toEmail);
            } catch (MessagingException | java.io.UnsupportedEncodingException e) {
                log.error("发送欢迎邮件失败: {}", e.getMessage());
            }
        });
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
