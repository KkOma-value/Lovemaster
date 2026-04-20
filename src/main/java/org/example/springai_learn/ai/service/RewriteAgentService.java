package org.example.springai_learn.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.ai.dto.RewriteRequest;
import org.example.springai_learn.ai.dto.RewriteResponse;
import org.example.springai_learn.ai.exception.RewriteException;
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
 * 仅在前端"优化提问"按钮显式触发时调用。
 * 职责：把用户口语化、零散的描述整理成信息更完整、表述更清晰的提问文本，就地替换回前端输入框。
 * 铁律：不改变用户意图、不加入用户没提到的细节、不输出"分析"或"建议"。
 */
@Service
@Slf4j
public class RewriteAgentService {

    private static final String TEXT_SYSTEM_PROMPT = """
            你是 Lovemaster 的 RewriteAgent。你只在用户点击"优化提问"按钮时被调用。

            你的任务：把用户口语化、零散的恋爱咨询描述，整理成一段信息更完整、
            表达更清晰的提问文本，让用户可以直接发送。

            铁律：
            - 不改变用户意图、不加入用户没提到的关系背景细节
            - 不输出"分析"或"建议"，只输出"更好的提问"
            - 保持第一人称、口语感、不说教
            - 输出长度 ≤ 原文的 2 倍，避免啰嗦

            只输出整理后的提问文本本身，不要 JSON、不要标签、不要前缀。
            """;

    private static final String IMAGE_SYSTEM_PROMPT = """
            你是 Lovemaster 的 RewriteAgent。你只在用户点击"优化提问"按钮时被调用。

            你的任务：结合聊天截图和用户的文字描述，整理成一段信息更完整、表达更清晰的提问文本。

            铁律：
            - 不改变用户意图、不加入用户没提到的关系背景细节
            - 不输出"分析"或"建议"，只输出"更好的提问"
            - 保持第一人称、口语感、不说教
            - 如果带了图片：先用 1 句话概括截图场景，再给出整理后的提问
            - 输出长度 ≤ 原文的 2 倍（算上场景概括），避免啰嗦

            只输出整理后的提问文本本身，不要 JSON、不要标签、不要前缀。
            """;

    private final ChatModel rewriteModel;
    private final ImagePathResolver imagePathResolver;

    public RewriteAgentService(
            @Qualifier("rewriteModel") ChatModel rewriteModel,
            ImagePathResolver imagePathResolver) {
        this.rewriteModel = rewriteModel;
        this.imagePathResolver = imagePathResolver;
    }

    public RewriteResponse optimize(RewriteRequest request, String userId) {
        String userMessage = request.userMessage();
        boolean hasImage = request.imageUrl() != null && !request.imageUrl().isBlank();

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(hasImage ? IMAGE_SYSTEM_PROMPT : TEXT_SYSTEM_PROMPT));

        try {
            if (hasImage) {
                Path imagePath = imagePathResolver.resolve(userId, request.imageUrl());
                MimeType mimeType = imagePathResolver.detectMimeType(imagePath);
                Media media = new Media(mimeType, new PathResource(imagePath));
                String userPrompt = "原始提问：%s\n\n请结合截图整理成更清晰的提问。".formatted(userMessage);
                messages.add(new UserMessage(userPrompt, media));
            } else {
                messages.add(new UserMessage("原始提问：%s\n\n请整理成更清晰的提问。".formatted(userMessage)));
            }

            ChatResponse response = rewriteModel.call(new Prompt(messages));
            String text = response.getResult().getOutput().getText();
            String cleaned = cleanOutput(text);
            if (cleaned.isBlank()) {
                throw new RewriteException("模型返回空内容");
            }
            log.info("RewriteAgent 完成: userId={}, 输入长度={}, 输出长度={}, 带图={}",
                    userId, userMessage.length(), cleaned.length(), hasImage);
            return new RewriteResponse(cleaned);
        } catch (RewriteException e) {
            throw e;
        } catch (Exception e) {
            log.warn("RewriteAgent 调用失败: userId={}, error={}", userId, e.getMessage());
            throw new RewriteException("优化失败：" + e.getMessage(), e);
        }
    }

    /**
     * 清理模型常见多余输出：Markdown 代码块围栏、前缀 label。
     */
    private String cleanOutput(String raw) {
        if (raw == null) return "";
        String t = raw.trim();
        if (t.startsWith("```")) {
            int firstNewline = t.indexOf('\n');
            if (firstNewline > 0) t = t.substring(firstNewline + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3).trim();
        }
        if (t.startsWith("整理后的提问：")) t = t.substring("整理后的提问：".length()).trim();
        if (t.startsWith("提问：")) t = t.substring("提问：".length()).trim();
        return t.trim();
    }
}
