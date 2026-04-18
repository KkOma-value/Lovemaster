package org.example.springai_learn.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.config.KnowledgeProperties;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 对 RAG 检索结果做最终清洗，去除潜在 prompt injection 与危险链接。
 * 召回片段直接拼进 system prompt，必须经过本类处理。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgePromptSanitizer {

    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("(?is)<\\s*script\\b[^>]*>.*?<\\s*/\\s*script\\s*>"),
            Pattern.compile("(?is)<\\s*iframe\\b[^>]*>.*?<\\s*/\\s*iframe\\s*>"),
            Pattern.compile("(?i)javascript\\s*:"),
            Pattern.compile("(?i)data\\s*:\\s*text\\s*/\\s*html"),
            Pattern.compile("(?i)on(click|load|error|mouseover)\\s*="),
            Pattern.compile("(?i)ignore\\s+(all\\s+)?(previous|prior|above)\\s+(instructions|messages|prompts)"),
            Pattern.compile("忽略(以上|之前|前面)(所有)?(指令|提示|系统提示)"),
            Pattern.compile("(?i)\\bsystem\\s*(:|prompt)\\s*\\{?"),
            Pattern.compile("(?i)you\\s+are\\s+now\\s+[^.\\n]{0,40}(developer|admin|root|god)\\s+mode"),
            Pattern.compile("(?i)disregard\\s+(the\\s+)?(above|previous)\\s+\\w+")
    );

    private final KnowledgeProperties properties;

    /**
     * 返回清洗结果，包含干净内容与命中计数。
     */
    public SanitizeResult sanitize(String content) {
        KnowledgeProperties.Sanitizer config = properties.getSanitizer();
        if (!config.isEnabled() || content == null || content.isBlank()) {
            return new SanitizeResult(content == null ? "" : content, 0);
        }

        String working = content;
        int blocked = 0;
        for (Pattern pattern : INJECTION_PATTERNS) {
            java.util.regex.Matcher matcher = pattern.matcher(working);
            if (matcher.find()) {
                int matches = 0;
                StringBuilder sb = new StringBuilder();
                matcher.reset();
                while (matcher.find()) {
                    matches++;
                    matcher.appendReplacement(sb, "[knowledge_redacted]");
                }
                matcher.appendTail(sb);
                if (matches > 0) {
                    blocked += matches;
                    working = sb.toString();
                }
            }
        }

        int cap = Math.max(256, config.getMaxContentChars());
        if (working.length() > cap) {
            working = working.substring(0, cap);
        }

        if (blocked > 0) {
            log.warn("Knowledge sanitizer blocked {} injection pattern hit(s), outChars={}", blocked, working.length());
        }
        return new SanitizeResult(working, blocked);
    }

    public record SanitizeResult(String content, int blockedHits) {
    }
}
