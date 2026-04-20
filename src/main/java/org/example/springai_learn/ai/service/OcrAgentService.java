package org.example.springai_learn.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.ai.context.OcrExtractionResult;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.Media;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.PathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 单职责 OCR Agent。只抽取截图里的文字 + 场景概述，不改写用户问题。
 * 主链路 Orchestrator 在有图片时调用，结果与用户原文一起塞给 RAG / BrainAgent。
 */
@Service
@Slf4j
public class OcrAgentService {

    private static final String SYSTEM_PROMPT = """
            你是 Lovemaster 的 OcrAgent。职责单一：阅读聊天截图，如实抽取内容。

            严格约束：
            - 不改写用户原问题
            - 不对"下一步怎么做"做判断
            - 不猜测对方心意（交给 BrainAgent）

            请按以下格式输出：

            OCR_TEXT: <按 A: ... B: ... 的格式列出关键对话轮次，≤200 字。看不清的字用 [?] 标注，不要猜>

            SCENE_SUMMARY: <2-3 句客观描述：谁在和谁对话、对话主题、关键情绪转折>

            UNREADABLE:
            - <如有看不清或不确定的地方，逐条列出；无则写"无">
            """;

    private final ChatModel visionModel;
    private final ImagePathResolver imagePathResolver;

    public OcrAgentService(
            @Qualifier("rewriteModel") ChatModel visionModel,
            ImagePathResolver imagePathResolver) {
        this.visionModel = visionModel;
        this.imagePathResolver = imagePathResolver;
    }

    public OcrExtractionResult extract(String imageUrl, String userId, String userMessage) {
        try {
            Path imagePath = imagePathResolver.resolve(userId, imageUrl);
            MimeType mimeType = imagePathResolver.detectMimeType(imagePath);
            Media media = new Media(mimeType, new PathResource(imagePath));

            String userPrompt = """
                    用户的文字描述（仅供理解场景，不要据此改写问题）：%s

                    请仅抽取截图中的对话内容与场景。
                    """.formatted(safeText(userMessage));

            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(SYSTEM_PROMPT));
            messages.add(new UserMessage(userPrompt, media));

            ChatResponse response = visionModel.call(new Prompt(messages));
            String raw = response.getResult().getOutput().getText();

            String ocrText = findSection(raw, "OCR_TEXT:", "SCENE_SUMMARY:", "UNREADABLE:");
            String sceneSummary = findSection(raw, "SCENE_SUMMARY:", "UNREADABLE:");

            if (ocrText.isBlank() && sceneSummary.isBlank()) {
                log.warn("OcrAgent 返回空内容: imageUrl={}", imageUrl);
                return OcrExtractionResult.failed("模型未返回可用内容");
            }
            return OcrExtractionResult.of(ocrText, sceneSummary);
        } catch (Exception e) {
            log.warn("OcrAgent 抽取失败: imageUrl={}, error={}", imageUrl, e.getMessage());
            return OcrExtractionResult.failed(e.getMessage());
        }
    }

    private String findSection(String raw, String label, String... nextLabels) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        int start = raw.indexOf(label);
        if (start < 0) {
            return "";
        }
        int sectionStart = start + label.length();
        int sectionEnd = raw.length();
        for (String nextLabel : nextLabels) {
            int candidate = raw.indexOf(nextLabel, sectionStart);
            if (candidate >= 0 && candidate < sectionEnd) {
                sectionEnd = candidate;
            }
        }
        return raw.substring(sectionStart, sectionEnd).trim();
    }

    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }
}
