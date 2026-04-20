package org.example.springai_learn.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.ai.dto.RewriteRequest;
import org.example.springai_learn.ai.dto.RewriteResponse;
import org.example.springai_learn.ai.exception.RateLimitExceededException;
import org.example.springai_learn.ai.service.RewriteAgentService;
import org.example.springai_learn.ai.service.RewriteRateLimiter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
@CrossOrigin(originPatterns = "*", allowCredentials = "false")
@Slf4j
public class RewriteController {

    private final RewriteAgentService rewriteAgentService;
    private final RewriteRateLimiter rateLimiter;

    public RewriteController(RewriteAgentService rewriteAgentService,
                             RewriteRateLimiter rateLimiter) {
        this.rewriteAgentService = rewriteAgentService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/rewrite")
    public RewriteResponse rewrite(@Valid @RequestBody RewriteRequest request,
                                   HttpServletRequest servletRequest) {
        String userId = resolveUserId();
        String ip = resolveClientIp(servletRequest);

        if (!rateLimiter.tryAcquire(userId, ip)) {
            log.warn("Rewrite 限流: userId={}, ip={}", userId, ip);
            throw new RateLimitExceededException("优化请求过于频繁，请稍后再试");
        }

        long start = System.currentTimeMillis();
        RewriteResponse response = rewriteAgentService.optimize(request, userId);
        log.info("Rewrite 完成: userId={}, 耗时={}ms, 输入长度={}, 输出长度={}",
                userId,
                System.currentTimeMillis() - start,
                request.userMessage().length(),
                response.optimizedText().length());
        return response;
    }

    private String resolveUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return "anonymous";
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof String principalStr
                && !principalStr.isBlank()
                && !"anonymousUser".equalsIgnoreCase(principalStr)) {
            return principalStr;
        }

        String userId = auth.getName();
        if (userId != null && !userId.isBlank() && !"anonymousUser".equalsIgnoreCase(userId)) {
            return userId;
        }

        return "anonymous";
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}
