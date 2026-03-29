package org.example.springai_learn.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.ai.context.ChatInputContext;
import org.example.springai_learn.ai.context.IntakeAnalysisResult;
import org.example.springai_learn.auth.service.ImageStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.Media;
import org.springframework.core.io.PathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MultimodalIntakeService {

    private static final String TEXT_ONLY_SYSTEM_PROMPT = """
            你是 Lovemaster 的 IntakeRewriteAgent。
            你的任务是把用户的恋爱问题整理成适合后续分析的结构化输入。

            请严格按以下格式输出：
            SUMMARY: <对上下文的简要整理>
            REWRITTEN_QUESTION: <重写后的标准问题>
            UNCERTAINTIES:
            - <不确定项1>
            - <不确定项2>
            INTENT: <用户当前更想要的结果>
            TOOL_HINT: <yes 或 no>
            """;

    private static final String IMAGE_SYSTEM_PROMPT = """
            你是 Lovemaster 的 IntakeRewriteAgent。
            你会阅读恋爱聊天截图并结合用户问题做结构化整理。

            处理要求：
            1. 优先识别截图中的双方对话和语气。
            2. 不确定的字词不要瞎补，放进 UNCERTAINTIES。
            3. REWRITTEN_QUESTION 要改写成一个适合恋爱分析的明确问题。
            4. 如果用户明显是在请求搜索、规划、生成资料、多步骤执行，则 TOOL_HINT = yes，否则写 no。

            请严格按以下格式输出：
            SUMMARY: <对上下文和截图内容的整理>
            OCR_TEXT: <尽量精炼的截图文字摘录，不要过长>
            REWRITTEN_QUESTION: <重写后的标准问题>
            UNCERTAINTIES:
            - <不确定项1>
            - <不确定项2>
            INTENT: <用户当前更想要的结果>
            TOOL_HINT: <yes 或 no>
            """;

    private final ChatModel dashscopeChatModel;

    @Autowired(required = false)
    private ImageStorageService imageStorageService;

    public IntakeAnalysisResult analyze(ChatInputContext context) {
        if (context.hasImage()) {
            try {
                return analyzeWithImage(context);
            } catch (Exception e) {
                log.warn("图片 intake 失败，降级为文本整理: {}", e.getMessage());
            }
        }
        return analyzeTextOnly(context);
    }

    private IntakeAnalysisResult analyzeTextOnly(ChatInputContext context) {
        String userPrompt = "用户问题：" + safeText(context.userMessage()) + "\n请整理为结构化分析输入。";
        String raw = invokeModel(List.of(
                new SystemMessage(TEXT_ONLY_SYSTEM_PROMPT),
                new UserMessage(userPrompt)
        ));
        return parse(raw, context, null);
    }

    private IntakeAnalysisResult analyzeWithImage(ChatInputContext context) throws Exception {
        if (imageStorageService == null) {
            throw new IllegalStateException("本地未启用图片存储服务，无法解析图片 URL");
        }
        Path imagePath = resolveImagePath(context.userId(), context.imageUrl());
        MimeType mimeType = detectMimeType(imagePath);
        Media media = new Media(mimeType, new PathResource(imagePath));
        String userPrompt = """
                用户问题：%s

                请结合截图判断双方在聊什么、语气如何、用户现在真正想解决什么问题。
                """.formatted(safeText(context.userMessage()));
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(IMAGE_SYSTEM_PROMPT));
        messages.add(new UserMessage(userPrompt, media));
        String raw = invokeModel(messages);
        return parse(raw, context, imagePath);
    }

    private String invokeModel(List<Message> messages) {
        ChatResponse response = dashscopeChatModel.call(new Prompt(messages));
        return response.getResult().getOutput().getText();
    }

    private IntakeAnalysisResult parse(String raw, ChatInputContext context, Path imagePath) {
        String summary = findValue(raw, "SUMMARY:");
        String ocrText = findValue(raw, "OCR_TEXT:");
        String rewrittenQuestion = findValue(raw, "REWRITTEN_QUESTION:");
        String intent = findValue(raw, "INTENT:");
        boolean toolHint = "yes".equalsIgnoreCase(findValue(raw, "TOOL_HINT:"));
        List<String> uncertainties = findBulletSection(raw, "UNCERTAINTIES:");

        if (summary.isBlank()) {
            summary = context.hasImage()
                    ? "用户提供了一张聊天截图，希望分析对方意思并获得下一步建议。"
                    : "用户希望获得一段恋爱关系分析和回复建议。";
        }
        if (rewrittenQuestion.isBlank()) {
            rewrittenQuestion = fallbackRewrite(context.userMessage(), context.hasImage());
        }
        if (intent.isBlank()) {
            intent = context.mode().name().equals("COACH") ? "判断是否需要进一步执行任务" : "理解对方意思并提供回复建议";
        }
        if (ocrText.isBlank() && imagePath != null) {
            ocrText = "系统已读取截图文件，模型未返回稳定的 OCR 摘录。";
            uncertainties = appendUncertainty(uncertainties, "截图文字摘录不完整，回答基于有限的图像理解。");
        }

        return new IntakeAnalysisResult(
                context.hasImage(),
                ocrText,
                summary,
                rewrittenQuestion,
                uncertainties,
                intent,
                toolHint || heuristicToolNeed(context.userMessage())
        );
    }

    private Path resolveImagePath(String userId, String imageUrl) throws Exception {
        String path = URI.create(imageUrl).getPath();
        String[] parts = path.split("/");
        if (parts.length < 5) {
            throw new IllegalArgumentException("无法解析图片 URL: " + imageUrl);
        }
        String parsedUserId = parts[parts.length - 3];
        String imageType = parts[parts.length - 2];
        String fileName = parts[parts.length - 1];
        if (userId != null && !userId.isBlank() && !"anonymous".equals(userId) && !userId.equals(parsedUserId)) {
            log.warn("图片 URL userId 与当前用户不一致，使用 URL 中的 userId 解析: current={}, url={}", userId, parsedUserId);
        }
        return imageStorageService.getImagePath(parsedUserId, imageType, fileName);
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

    private String findValue(String raw, String label) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        int start = raw.indexOf(label);
        if (start < 0) {
            return "";
        }
        int valueStart = start + label.length();
        int lineEnd = raw.indexOf('\n', valueStart);
        if (lineEnd < 0) {
            lineEnd = raw.length();
        }
        return raw.substring(valueStart, lineEnd).trim();
    }

    private List<String> findBulletSection(String raw, String label) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        int start = raw.indexOf(label);
        if (start < 0) {
            return List.of();
        }
        String tail = raw.substring(start + label.length()).trim();
        List<String> bullets = new ArrayList<>();
        for (String line : tail.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            if (!trimmed.startsWith("-")) {
                break;
            }
            bullets.add(trimmed.substring(1).trim());
        }
        return bullets;
    }

    private List<String> appendUncertainty(List<String> uncertainties, String extra) {
        List<String> updated = new ArrayList<>(uncertainties);
        updated.add(extra);
        return updated;
    }

    private String fallbackRewrite(String userMessage, boolean hasImage) {
        if (hasImage) {
            return "请结合这张聊天截图，判断对方当前的情绪和意图，并给出我下一步更合适的回复。";
        }
        return "请根据我的描述，判断对方的真实意思，并给我可直接发送的回复建议：" + safeText(userMessage);
    }

    private boolean heuristicToolNeed(String userMessage) {
        if (userMessage == null) {
            return false;
        }
        String text = userMessage.toLowerCase();
        return text.contains("计划")
                || text.contains("方案")
                || text.contains("帮我查")
                || text.contains("搜索")
                || text.contains("整理")
                || text.contains("生成")
                || text.contains("pdf")
                || text.contains("文档");
    }

    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }
}
