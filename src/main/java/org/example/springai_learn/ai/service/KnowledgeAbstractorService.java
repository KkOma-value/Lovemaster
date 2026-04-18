package org.example.springai_learn.ai.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.config.KnowledgeProperties;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class KnowledgeAbstractorService {

    private static final String SYSTEM_PROMPT = """
            你是恋爱咨询知识蒸馏专家。请把下面的问答对提炼为结构化经验，严格输出 JSON 对象，不要输出任何多余文字或 Markdown 代码块标记。
            字段约束：
            - scenario: 场景描述（20-60字）
            - cause: 造成该场景的常见成因（20-60字）
            - strategy: 建议的应对策略（30-80字）
            - taboo: 需要避免的做法（20-60字）
            - summary: 一句话总结（<=60字，用于展示）
            若信息不足，对应字段填 "未提供" 保持 JSON 合法。
            """;

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final KnowledgeProperties properties;

    public KnowledgeAbstractorService(
            @Qualifier("dashscopeChatModel") ChatModel chatModel,
            ObjectMapper objectMapper,
            KnowledgeProperties properties
    ) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public StructuredKnowledge summarize(String question, String answer) {
        KnowledgeProperties.Abstractor config = properties.getAbstractor();
        String safeQuestion = truncate(question, config.getMaxInputChars());
        String safeAnswer = truncate(answer, config.getMaxInputChars());

        if (!config.isEnabled() || safeAnswer.isEmpty()) {
            return fallback(safeQuestion, safeAnswer, config.getMaxSummaryChars());
        }

        try {
            String userContent = "问题: " + (safeQuestion.isEmpty() ? "(未提供)" : safeQuestion)
                    + "\n回答: " + safeAnswer;
            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(SYSTEM_PROMPT),
                    new UserMessage(userContent)
            ));
            String raw = chatModel.call(prompt).getResult().getOutput().getText();
            String cleaned = stripJsonFence(raw);
            StructuredKnowledge parsed = objectMapper.readValue(cleaned, StructuredKnowledge.class);
            return normalize(parsed, safeQuestion, safeAnswer, config.getMaxSummaryChars());
        } catch (JsonProcessingException ex) {
            log.warn("KnowledgeAbstractor JSON parse failed: {}", ex.getMessage());
            return fallback(safeQuestion, safeAnswer, config.getMaxSummaryChars());
        } catch (RuntimeException ex) {
            log.warn("KnowledgeAbstractor LLM call failed: {}", ex.getMessage());
            return fallback(safeQuestion, safeAnswer, config.getMaxSummaryChars());
        }
    }

    public String toJson(StructuredKnowledge knowledge) {
        if (knowledge == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(knowledge);
        } catch (JsonProcessingException ex) {
            log.warn("Serialize StructuredKnowledge failed: {}", ex.getMessage());
            return knowledge.summary();
        }
    }

    static String stripJsonFence(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            int closing = trimmed.lastIndexOf("```");
            if (closing >= 0) {
                trimmed = trimmed.substring(0, closing);
            }
        }
        int jsonStart = trimmed.indexOf('{');
        int jsonEnd = trimmed.lastIndexOf('}');
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            return trimmed.substring(jsonStart, jsonEnd + 1);
        }
        return trimmed;
    }

    private StructuredKnowledge normalize(StructuredKnowledge in, String question, String answer, int maxSummary) {
        String scenario = firstNonBlank(in == null ? null : in.scenario(), "未提供");
        String cause = firstNonBlank(in == null ? null : in.cause(), "未提供");
        String strategy = firstNonBlank(in == null ? null : in.strategy(), "未提供");
        String taboo = firstNonBlank(in == null ? null : in.taboo(), "未提供");
        String summary = firstNonBlank(in == null ? null : in.summary(), buildShortSummary(question, answer));
        return new StructuredKnowledge(scenario, cause, strategy, taboo, truncate(summary, maxSummary));
    }

    private StructuredKnowledge fallback(String question, String answer, int maxSummary) {
        String summary = buildShortSummary(question, answer);
        return new StructuredKnowledge("未提供", "未提供", "未提供", "未提供", truncate(summary, maxSummary));
    }

    private static String buildShortSummary(String question, String answer) {
        String base = (question == null || question.isBlank()) ? answer : question + " | " + answer;
        if (base == null) {
            return "";
        }
        return base.replaceAll("\\s+", " ").trim();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        if (max <= 0 || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private static String firstNonBlank(String candidate, String fallback) {
        if (candidate == null) {
            return fallback;
        }
        String trimmed = candidate.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StructuredKnowledge(
            String scenario,
            String cause,
            String strategy,
            String taboo,
            String summary
    ) {
    }
}
