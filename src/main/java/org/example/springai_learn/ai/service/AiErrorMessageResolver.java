package org.example.springai_learn.ai.service;

import org.springframework.web.client.RestClientResponseException;

/**
 * Maps upstream AI/provider exceptions into user-facing messages.
 */
public final class AiErrorMessageResolver {

    private AiErrorMessageResolver() {
    }

    public static String resolve(Throwable throwable) {
        RestClientResponseException responseException = findCause(throwable, RestClientResponseException.class);
        if (responseException != null) {
            int status = responseException.getStatusCode().value();
            if (status == 401 || status == 403) {
                return "AI 服务鉴权失败，DashScope 返回了 401/403。请更新 application-local.yml 中的 spring.ai.dashscope.api-key，或改用环境变量 DASHSCOPE_API_KEY / AI_DASHSCOPE_API_KEY。";
            }
            if (status == 429) {
                return "AI 服务当前限流，请稍后重试。";
            }
            if (status >= 500) {
                return "AI 服务暂时不可用，请稍后重试。";
            }
        }

        String message = findMessage(throwable);
        if (message == null || message.isBlank()) {
            return "AI 服务调用失败，请稍后重试。";
        }

        String lowered = message.toLowerCase();
        if (lowered.contains("401") || lowered.contains("403") || lowered.contains("unauthorized")
                || lowered.contains("forbidden")) {
            return "AI 服务鉴权失败，DashScope 返回了 401/403。请更新 application-local.yml 中的 spring.ai.dashscope.api-key，或改用环境变量 DASHSCOPE_API_KEY / AI_DASHSCOPE_API_KEY。";
        }
        if (lowered.contains("429") || lowered.contains("rate limit")) {
            return "AI 服务当前限流，请稍后重试。";
        }

        return message;
    }

    private static String findMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                return current.getMessage().trim();
            }
            current = current.getCause();
        }
        return null;
    }

    private static <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }
}
