package org.example.springai_learn.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@Slf4j
public class SseEventHelper {

    public void send(SseEmitter emitter, String type, String content) {
        if (emitter == null) {
            return;
        }
        try {
            String json = String.format("{\"type\":\"%s\",\"content\":\"%s\"}",
                    escape(type),
                    escape(content));
            emitter.send(json);
        } catch (Exception e) {
            log.warn("发送 SSE 事件失败: type={}, error={}", type, e.getMessage());
        }
    }

    public void done(SseEmitter emitter) {
        send(emitter, "done", "");
    }

    private String escape(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
