package org.example.springai_learn.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.ai.context.ChatInputContext;
import org.example.springai_learn.ai.context.IntakeAnalysisResult;
import org.example.springai_learn.auth.service.ImageStorageService;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
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
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class MultimodalIntakeService {

    private static final String TEXT_ONLY_SYSTEM_PROMPT = """
            你是 Lovemaster 的 IntakeRewriteAgent，一位擅长理解情感问题的专业助手。

            你的任务是把用户的恋爱/情感问题整理成结构化的分析输入，帮助后续的 BrainAgent 更好地理解用户的处境和需求。

            整理原则：
            - 像朋友一样理解用户的处境，但保持专业分析视角
            - 捕捉用户描述中的情感线索和关系动态
            - 明确用户真正想问的核心问题
            - 识别用户是否需要外部工具协助（如搜索约会地点、生成回复模板等）

            请按以下格式输出：

            SUMMARY: <用2-3句话概括用户的处境和核心矛盾，包括关系背景、当前状态、用户的主要困扰>

            REWRITTEN_QUESTION: <将用户的问题改写成清晰、具体的分析型问题。如果用户描述模糊，补充合理的上下文假设。问题应该聚焦于"对方是什么意思""我该怎么回应""这段关系该如何推进"等方向>

            UNCERTAINTIES:
            - <信息缺失1：用户没交代清楚但影响判断的关键信息>
            - <信息缺失2：关系背景、认识时长、对方性格等不确定因素>

            INTENT: <用户此刻最想要的结果：是想理解对方心意？寻求回复建议？评估关系前景？还是情绪疏导？>

            TOOL_HINT: <yes 或 no - 如果用户需要搜索信息、制定计划、生成文档/图片、多步骤执行等，填yes；如果只是咨询建议，填no>
            """;

    private static final String IMAGE_SYSTEM_PROMPT = """
            你是 Lovemaster 的 IntakeRewriteAgent，专门分析恋爱聊天截图的情感助手。

            你的任务是仔细阅读聊天截图，结合用户的补充描述，整理出结构化的分析输入。

            分析重点：
            - 识别对话双方的身份关系（暧昧期/情侣/前任/追求者等）
            - 捕捉语气细节：冷淡、热情、敷衍、试探、撒娇、生气等情绪信号
            - 关注回复节奏：谁主动、间隔多久、字数对比等权力动态
            - 注意表情包、标点、省略号等隐含情绪
            - 不确定的内容如实标注，不要猜测填充

            请按以下格式输出：

            SUMMARY: <用2-3句话概括聊天场景：双方关系状态、对话主题、关键情绪转折点、用户的核心困惑>

            OCR_TEXT: <精炼的聊天文字摘录，保留关键对话轮次，格式建议：A: ... B: ... 。只摘录对分析有帮助的内容，控制在200字以内>

            REWRITTEN_QUESTION: <结合截图内容，将用户问题改写成具体的分析型问题。例如："对方回复变慢且只用表情，是在冷淡我吗？""对方说'随便你'但发了可爱表情，真实态度是什么？">

            UNCERTAINTIES:
            - <截图中看不清或不确定的内容，如模糊的字、不确定的表情包含义>
            - <关系背景缺失，如认识多久、是否见过面、之前聊得如何>

            INTENT: <用户此刻最想要的结果：理解对方真实想法？寻求回复话术？判断关系走向？还是确认自己是否过度解读？>

            TOOL_HINT: <yes 或 no - 如果用户需要搜索约会地点、生成回复模板、整理聊天记录分析等，填yes；如果只是咨询建议，填no>
            """;

    private final ChatModel rewriteModel;

    @Autowired(required = false)
    private ImageStorageService imageStorageService;

    private final RestTemplate restTemplate = new RestTemplate();

    public MultimodalIntakeService(@Qualifier("rewriteModel") ChatModel rewriteModel) {
        this.rewriteModel = rewriteModel;
    }

    public IntakeAnalysisResult analyze(ChatInputContext context) {
        if (context.hasImage()) {
            try {
                return analyzeWithImage(context);
            } catch (Exception e) {
                log.warn("图片 intake 失败，降级为文本整理: chatId={}, error={}", context.chatId(), e.getMessage());
            }
        }
        try {
            return analyzeTextOnly(context);
        } catch (Exception e) {
            log.warn("文本 intake 失败，启用本地兜底: chatId={}, error={}", context.chatId(), e.getMessage());
            return buildLocalFallback(context, e);
        }
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
        ChatResponse response = rewriteModel.call(new Prompt(messages));
        return response.getResult().getOutput().getText();
    }

    private IntakeAnalysisResult buildLocalFallback(ChatInputContext context, Exception exception) {
        boolean likelyNeedTools = heuristicToolNeed(context.userMessage());
        String safeUserMessage = safeText(context.userMessage());
        String summary = context.hasImage()
                ? "用户提供了聊天截图，并希望结合补充描述继续分析当前关系动态或下一步行动。"
                : buildFallbackSummary(safeUserMessage, likelyNeedTools);
        String rewrittenQuestion = buildFallbackRewrite(safeUserMessage, context.hasImage(), likelyNeedTools);
        String intent = inferIntent(safeUserMessage, likelyNeedTools);
        String ocrText = context.hasImage()
                ? "截图暂未识别成功，当前先根据你的文字描述继续分析。"
                : "";

        List<String> uncertainties = new ArrayList<>();
        uncertainties.add("上游模型整理阶段暂时不可用，当前结果基于原始输入进行本地兜底分析。");
        String errorMessage = safeText(exception.getMessage());
        if (!errorMessage.isBlank()) {
            uncertainties.add("模型异常：" + abbreviate(errorMessage, 120));
        }

        return new IntakeAnalysisResult(
                context.hasImage(),
                ocrText,
                summary,
                rewrittenQuestion,
                uncertainties,
                intent,
                likelyNeedTools
        );
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
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("图片 URL 为空");
        }

        // If it's a public HTTP URL, download to temp file
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            log.info("检测到 public 图片 URL，开始下载到临时文件: {}", imageUrl);
            byte[] imageBytes = restTemplate.getForEntity(imageUrl, byte[].class).getBody();
            if (imageBytes == null) {
                throw new IllegalStateException("无法从 URL 下载图片: " + imageUrl);
            }
            String ext = guessExtensionFromUrl(imageUrl);
            Path tempFile = Files.createTempFile("lovemaster_img_", ext);
            Files.write(tempFile, imageBytes);
            log.info("图片已下载到临时文件: {}", tempFile);
            return tempFile;
        }

        // Legacy local URL format: /api/images/{userId}/{imageType}/{fileName}
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

    private String guessExtensionFromUrl(String imageUrl) {
        String lower = imageUrl.toLowerCase();
        if (lower.contains(".png")) return ".png";
        if (lower.contains(".jpg") || lower.contains(".jpeg")) return ".jpg";
        if (lower.contains(".webp")) return ".webp";
        if (lower.contains(".gif")) return ".gif";
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

    private String buildFallbackRewrite(String userMessage, boolean hasImage, boolean likelyNeedTools) {
        if (hasImage) {
            return fallbackRewrite(userMessage, true);
        }
        if (likelyNeedTools) {
            return "请根据用户原始需求，整理出需要查询或推荐的信息，并给出可直接使用的结果：" + userMessage;
        }
        return fallbackRewrite(userMessage, false);
    }

    private String buildFallbackSummary(String userMessage, boolean likelyNeedTools) {
        if (userMessage.isBlank()) {
            return "用户希望获得一段恋爱关系分析和回复建议。";
        }
        if (likelyNeedTools) {
            return "用户希望围绕恋爱场景获取外部信息支持，例如地点推荐、活动建议、攻略或参考资料。";
        }
        return "用户正在描述一段情感互动，希望获得对对方态度的理解和下一步建议。";
    }

    private String inferIntent(String userMessage, boolean likelyNeedTools) {
        if (likelyNeedTools) {
            return "获取带有外部信息支持的推荐、攻略或执行建议";
        }
        if (userMessage.contains("怎么回") || userMessage.contains("回复")) {
            return "获得合适的回复建议";
        }
        if (userMessage.contains("什么意思") || userMessage.contains("怎么想")) {
            return "理解对方真实想法";
        }
        return "理解当前关系状态并获得下一步建议";
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
                || text.contains("推荐")
                || text.contains("攻略")
                || text.contains("景点")
                || text.contains("地点")
                || text.contains("餐厅")
                || text.contains("旅游")
                || text.contains("旅行")
                || text.contains("照片")
                || text.contains("图片")
                || text.contains("看看")
                || text.contains("整理")
                || text.contains("生成")
                || text.contains("pdf")
                || text.contains("文档");
    }

    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }

    private String abbreviate(String text, int limit) {
        if (text == null || text.length() <= limit) {
            return safeText(text);
        }
        return text.substring(0, limit) + "...";
    }
}
