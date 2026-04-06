package org.example.springai_learn.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class SseEventHelper {

    private final ObjectMapper objectMapper;

    /**
     * 记录已断开的 emitter，避免重复写入死连接导致 dispatcherServlet 报错。
     * 使用 identity-based Set (比较对象引用) 而非 equals。
     */
    private final Set<SseEmitter> deadEmitters = ConcurrentHashMap.newKeySet();

    public void send(SseEmitter emitter, String type, String content) {
        send(emitter, type, content, null);
    }

    public void send(SseEmitter emitter, String type, String content, Map<String, Object> data) {
        if (emitter == null || deadEmitters.contains(emitter)) {
            return;
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", type);
            payload.put("content", content == null ? "" : content);
            if (data != null && !data.isEmpty()) {
                payload.put("data", data);
            }
            emitter.send(objectMapper.writeValueAsString(payload));
        } catch (IOException e) {
            // 客户端已断开 — 标记为死连接并通知 Spring 清理异步处理
            log.debug("SSE 连接已断开，停止发送: type={}", type);
            markDead(emitter, e);
        } catch (IllegalStateException e) {
            // emitter 已 complete — 静默忽略
            log.debug("SSE emitter 已关闭: type={}", type);
            deadEmitters.add(emitter);
        } catch (Exception e) {
            log.warn("发送 SSE 事件失败: type={}, error={}", type, e.getMessage());
        }
    }

    public void done(SseEmitter emitter) {
        send(emitter, "done", "");
    }

    public void done(SseEmitter emitter, Map<String, Object> data) {
        send(emitter, "done", "", data);
    }

    /**
     * 注册 emitter 生命周期回调，在连接结束时自动清理。
     * 应在创建 emitter 后立即调用。
     */
    public void registerLifecycle(SseEmitter emitter) {
        emitter.onCompletion(() -> deadEmitters.remove(emitter));
        emitter.onError(e -> deadEmitters.add(emitter));
        emitter.onTimeout(() -> deadEmitters.add(emitter));
    }

    private void markDead(SseEmitter emitter, IOException cause) {
        deadEmitters.add(emitter);
        try {
            emitter.completeWithError(cause);
        } catch (Exception ignored) {
            // 已经 complete 了，忽略
        }
    }
}
