package org.example.springai_learn.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class SseEventHelper {

    private final ObjectMapper objectMapper;

    public void send(SseEmitter emitter, String type, String content) {
        send(emitter, type, content, null);
    }

    public void send(SseEmitter emitter, String type, String content, Map<String, Object> data) {
        if (emitter == null) {
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
}
