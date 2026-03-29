package org.example.springai_learn.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DashScopeStartupValidator implements ApplicationRunner {

    private static final Set<String> PLACEHOLDER_KEYS = Set.of(
            "your_dashscope_api_key",
            "YOUR_DASHSCOPE_API_KEY_HERE"
    );

    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        String globalApiKey = normalize(environment.getProperty("spring.ai.dashscope.api-key"));
        String chatApiKey = normalize(environment.getProperty("spring.ai.dashscope.chat.api-key"));
        String effectiveApiKey = StringUtils.hasText(chatApiKey) ? chatApiKey : globalApiKey;
        String model = normalize(environment.getProperty("spring.ai.dashscope.chat.options.model"));

        if (!StringUtils.hasText(effectiveApiKey)) {
            log.error("DashScope chat 未配置 API key，请在 application-local.yml 或环境变量 DASHSCOPE_API_KEY / AI_DASHSCOPE_API_KEY 中设置。");
            return;
        }

        if (PLACEHOLDER_KEYS.contains(effectiveApiKey)) {
            throw new IllegalStateException("DashScope API key 仍是模板占位值，请替换为真实 key 后再启动。");
        }

        log.info("DashScope chat 配置已加载: model={}, apiKey={}",
                StringUtils.hasText(model) ? model : "<provider-default>",
                mask(effectiveApiKey));
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String mask(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return "<empty>";
        }
        if (apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
