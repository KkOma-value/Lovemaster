package org.example.springai_learn.ai.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 把 RewriteController 抛出的受控异常映射成稳定的 JSON 响应体。
 */
@RestControllerAdvice(basePackages = "org.example.springai_learn.controller")
@Slf4j
public class RewriteExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, String>> handleRateLimit(RateLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("error", "RATE_LIMITED", "message", ex.getMessage()));
    }

    @ExceptionHandler(RewriteException.class)
    public ResponseEntity<Map<String, String>> handleRewrite(RewriteException ex) {
        log.warn("Rewrite 失败: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "REWRITE_FAILED", "message", "AI 整理失败，请直接发送"));
    }
}
