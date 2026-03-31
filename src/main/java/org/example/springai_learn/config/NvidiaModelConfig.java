package org.example.springai_learn.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * NVIDIA NIM 模型配置 — 通过 OpenAI 兼容接口创建三个独立的 ChatModel Bean。
 *
 * <ul>
 *   <li>rewriteModel → Qwen3.5 VLM 122B (用于 MultimodalIntakeService 的问题重写，支持图像识别)</li>
 *   <li>toolsModel   → DeepSeek-R1 (用于 ToolsAgentService 工具调用)</li>
 *   <li>brainModel   → Kimi-K2-Thinking (用于 Coach 模式的直接回答)</li>
 * </ul>
 */
@Configuration
@Slf4j
public class NvidiaModelConfig {

    @Value("${spring.ai.openai.base-url:https://integrate.api.nvidia.com}")
    private String baseUrl;

    @Value("${spring.ai.openai.api-key:placeholder}")
    private String apiKey;

    // ---- model names (configurable via application-local.yml or env vars) ----

    @Value("${nvidia.model.rewrite:qwen/qwen3.5-122b-a10b}")
    private String rewriteModelName;

    @Value("${nvidia.model.tools:deepseek-ai/deepseek-r1}")
    private String toolsModelName;

    @Value("${nvidia.model.brain:moonshotai/kimi-k2-thinking}")
    private String brainModelName;

    // =========================================================================
    //  rewriteModel — Qwen3.5 VLM 122B
    // =========================================================================

    @Bean("rewriteModel")
    public ChatModel rewriteModel() {
        log.info("Creating NVIDIA rewriteModel: {} @ {}", rewriteModelName, baseUrl);
        OpenAiApi api = OpenAiApi.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(rewriteModelName)
                .temperature(0.3)
                .build();
        return new OpenAiChatModel(api, options);
    }

    // =========================================================================
    //  toolsModel — DeepSeek-R1 (用于 ToolsAgentService 工具调用)
    // =========================================================================

    @Bean("toolsModel")
    public ChatModel toolsModel() {
        log.info("Creating NVIDIA toolsModel: {} @ {}", toolsModelName, baseUrl);
        OpenAiApi api = OpenAiApi.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(toolsModelName)
                .temperature(0.6)
                .build();
        return new OpenAiChatModel(api, options);
    }

    // =========================================================================
    //  brainModel — Kimi-K2-Thinking
    // =========================================================================

    @Bean("brainModel")
    public ChatModel brainModel() {
        log.info("Creating NVIDIA brainModel: {} @ {}", brainModelName, baseUrl);
        OpenAiApi api = OpenAiApi.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(brainModelName)
                .temperature(0.7)
                .build();
        return new OpenAiChatModel(api, options);
    }
}
