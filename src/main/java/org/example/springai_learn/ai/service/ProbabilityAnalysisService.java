package org.example.springai_learn.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.ai.context.ChatInputContext;
import org.example.springai_learn.ai.context.IntakeAnalysisResult;
import org.example.springai_learn.ai.context.ProbabilityAnalysis;
import org.example.springai_learn.auth.service.ImageStorageService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.PathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 成功概率分析服务 — 调用 AI 模型对用户追求心仪对象的成功概率进行结构化评估。
 * 使用 Spring AI 的 .entity(ProbabilityAnalysis.class) 进行结构化输出。
 * 失败时返回 null，不抛出异常，编排器跳过概率卡片。
 */
@Service
@Slf4j
public class ProbabilityAnalysisService {

    private static final String SYSTEM_PROMPT = """
            你是 Lovemaster 的 ProbabilityAnalyst，专门评估"用户追求心仪对象成功的概率"。

            输入：
            - 用户原始问题
            - Intake 摘要（对话背景、关系阶段）
            - OCR 摘录（如果有聊天截图）
            - 重写后的问题
            - 截图（如果有）

            输出约束（必须严格遵守）：
            - 只输出 JSON，不输出任何 markdown、解释或额外文字
            - probability 必须在 15-85 之间（confidence=high 时允许 10-90）
            - greenFlags 至少 2 条，redFlags 至少 1 条，nextActions 恰好 3 条
            - 每个 flag 必须引用具体证据（截图里哪句话 / 用户描述中哪个事实）
            - summary 用 1-2 句口语化总结，不要撒娇
            - tier 分档规则：0-29 极低 / 30-44 偏低 / 45-59 一般 / 60-74 较高 / 75-100 很高
            - confidence 规则：无截图且描述模糊→low；有截图且信息充分→high；其他→medium
            - weight 可选值：low / medium / high
            - nextActions 每条必须是"可以立即执行的具体动作"，不要"保持自信"这种空话
            - nextActions 的 tone 可选值：主动 / 温和 / 稳健 / 克制 / 有趣
            - 性别中立：用"TA"指代心仪对象，不预设性别
            """;

    private final ChatClient chatClient;

    @Autowired(required = false)
    private ImageStorageService imageStorageService;

    private final RestTemplate restTemplate = new RestTemplate();

    public ProbabilityAnalysisService(@Qualifier("rewriteModel") ChatModel rewriteModel) {
        this.chatClient = ChatClient.builder(rewriteModel).build();
    }

    /**
     * 分析成功概率。失败时返回 null（不抛异常）。
     */
    public ProbabilityAnalysis analyze(ChatInputContext context, IntakeAnalysisResult intake) {
        try {
            String userPrompt = buildUserPrompt(context, intake);

            if (context.hasImage() && imageStorageService != null) {
                try {
                    Path imagePath = resolveImagePath(context.userId(), context.imageUrl());
                    MimeType mimeType = detectMimeType(imagePath);
                    Media media = new Media(mimeType, new PathResource(imagePath));

                    return chatClient.prompt()
                            .system(SYSTEM_PROMPT)
                            .user(spec -> spec.text(userPrompt).media(media))
                            .call()
                            .entity(ProbabilityAnalysis.class);
                } catch (Exception e) {
                    log.warn("概率分析附带图片失败，降级为纯文本分析: {}", e.getMessage());
                }
            }

            // 无图片或图片加载失败：纯文本分析
            return chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .entity(ProbabilityAnalysis.class);

        } catch (Exception e) {
            log.warn("概率分析失败，跳过概率卡片: chatId={}, error={}", context.chatId(), e.getMessage());
            return null;
        }
    }

    private String buildUserPrompt(ChatInputContext context, IntakeAnalysisResult intake) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户原始问题：").append(safe(context.userMessage())).append("\n\n");
        sb.append("Intake 摘要：").append(safe(intake.conversationSummary())).append("\n");
        if (intake.ocrText() != null && !intake.ocrText().isBlank()) {
            sb.append("OCR 摘录：").append(intake.ocrText()).append("\n");
        }
        sb.append("重写后的问题：").append(safe(intake.rewrittenQuestion())).append("\n");
        if (!intake.hasImage()) {
            sb.append("\n注意：用户没有上传截图，请将 confidence 设为 low，并在 summary 中提示上传截图可获得更准确评估。\n");
        }
        sb.append("\n请根据以上信息，输出成功概率分析 JSON。");
        return sb.toString();
    }

    private Path resolveImagePath(String userId, String imageUrl) throws Exception {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("图片 URL 为空");
        }
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            byte[] imageBytes = restTemplate.getForEntity(imageUrl, byte[].class).getBody();
            if (imageBytes == null) {
                throw new IllegalStateException("无法从 URL 下载图片: " + imageUrl);
            }
            String ext = guessExtension(imageUrl);
            Path tempFile = Files.createTempFile("prob_img_", ext);
            Files.write(tempFile, imageBytes);
            return tempFile;
        }
        String path = URI.create(imageUrl).getPath();
        String[] parts = path.split("/");
        if (parts.length < 5) {
            throw new IllegalArgumentException("无法解析图片 URL: " + imageUrl);
        }
        String parsedUserId = parts[parts.length - 3];
        String imageType = parts[parts.length - 2];
        String fileName = parts[parts.length - 1];
        return imageStorageService.getImagePath(parsedUserId, imageType, fileName);
    }

    private String guessExtension(String url) {
        String lower = url.toLowerCase();
        if (lower.contains(".png")) return ".png";
        if (lower.contains(".jpg") || lower.contains(".jpeg")) return ".jpg";
        if (lower.contains(".webp")) return ".webp";
        return ".tmp";
    }

    private MimeType detectMimeType(Path imagePath) {
        try {
            String detected = Files.probeContentType(imagePath);
            if (detected != null && !detected.isBlank()) {
                return MimeTypeUtils.parseMimeType(detected);
            }
        } catch (Exception e) {
            log.debug("检测图片 MimeType 失败: {}", e.getMessage());
        }
        return MimeTypeUtils.IMAGE_PNG;
    }

    private String safe(String text) {
        return text == null ? "" : text.trim();
    }
}
