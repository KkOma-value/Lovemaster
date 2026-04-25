package org.example.springai_learn.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.auth.entity.WikiCandidate;
import org.example.springai_learn.auth.entity.WikiFeedbackEvent;
import org.example.springai_learn.auth.repository.WikiCandidateRepository;
import org.example.springai_learn.auth.repository.WikiFeedbackEventRepository;
import org.example.springai_learn.config.KnowledgeProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 周期扫描高分反馈事件，将对应的 WikiCandidate 落盘到 knowledge/wiki/inbox/，
 * 供 WikiKnowledgeService 热加载吸收。已处理的事件被打上 processed=true。
 * 不直接推送 Dify 以避免脏数据；人工或后续流程可决定是否推远端数据集。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KnowledgeReinforcementJob {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final List<String> DEFAULT_EVENT_TYPES = List.of("helpful", "candidate_submitted");

    private final KnowledgeProperties properties;
    private final WikiFeedbackEventRepository feedbackEventRepository;
    private final WikiCandidateRepository candidateRepository;
    private final KnowledgeMetrics metrics;
    private final WikiGraphSyncService graphSyncService;

    @Scheduled(cron = "${app.knowledge.feedback.cron:0 0 */6 * * *}")
    public void reinforceFromFeedback() {
        KnowledgeProperties.Feedback config = properties.getFeedback();
        if (!config.isEnabled()) {
            return;
        }

        BigDecimal minScore = BigDecimal.valueOf(config.getHelpfulThreshold());
        int batchSize = Math.max(10, config.getBatchSize());
        List<WikiFeedbackEvent> events = feedbackEventRepository.findUnprocessedHighScoreEvents(
                minScore,
                DEFAULT_EVENT_TYPES,
                PageRequest.of(0, batchSize)
        );
        if (events.isEmpty()) {
            log.info("Knowledge reinforcement: no unprocessed high-score events, minScore={}", minScore);
            return;
        }

        Map<String, List<WikiFeedbackEvent>> groupedByCandidate = events.stream()
                .filter(e -> e.getCandidateId() != null)
                .collect(Collectors.groupingBy(WikiFeedbackEvent::getCandidateId, LinkedHashMap::new, Collectors.toList()));

        int minRepeat = Math.max(1, config.getMinRepeat());
        int reinforced = 0;
        int skipped = 0;
        Set<String> processedEventIds = new HashSet<>();

        for (Map.Entry<String, List<WikiFeedbackEvent>> entry : groupedByCandidate.entrySet()) {
            String candidateId = entry.getKey();
            List<WikiFeedbackEvent> group = entry.getValue();
            if (group.size() < minRepeat) {
                skipped += group.size();
                continue;
            }

            WikiCandidate candidate = candidateRepository.findById(candidateId).orElse(null);
            if (candidate == null || candidate.getRawAnswer() == null || candidate.getRawAnswer().isBlank()) {
                log.warn("Candidate missing or empty, candidateId={}", candidateId);
                skipped += group.size();
                continue;
            }

            try {
                writeInboxFile(candidate, group, config.getInboxDir());
                reinforced += group.size();
                group.forEach(e -> processedEventIds.add(e.getId()));
            } catch (IOException ex) {
                log.warn("Failed to write inbox file for candidate {}: {}", candidateId, ex.getMessage());
                skipped += group.size();
            }
        }

        if (!processedEventIds.isEmpty()) {
            markProcessed(processedEventIds);
        }

        metrics.feedbackReinforced(reinforced);
        metrics.feedbackSkipped(skipped);
        log.info("Knowledge reinforcement sweep done, reinforced={}, skipped={}, candidates={}",
                reinforced, skipped, groupedByCandidate.size());

        // 写入新文件后触发 graphify 图谱同步
        if (reinforced > 0) {
            graphSyncService.requestSync("reinforcement-job");
        }
    }

    @Transactional
    protected void markProcessed(Set<String> eventIds) {
        List<WikiFeedbackEvent> toUpdate = feedbackEventRepository.findAllById(eventIds);
        LocalDateTime now = LocalDateTime.now();
        for (WikiFeedbackEvent event : toUpdate) {
            event.setProcessed(true);
            event.setProcessedAt(now);
        }
        feedbackEventRepository.saveAll(toUpdate);
    }

    private void writeInboxFile(WikiCandidate candidate, List<WikiFeedbackEvent> group, String inboxDir) throws IOException {
        Path dir = Path.of(inboxDir);
        Files.createDirectories(dir);

        // --- Step2: Jaccard dedup against existing inbox files ---
        double dedupThreshold = properties.getReview().getDedupThreshold();
        String candidateSummary = candidate.getAbstractSummary();
        if (candidateSummary != null && !candidateSummary.isBlank()) {
            try {
                List<String> existingTitles = readInboxTitles(dir);
                for (String existingTitle : existingTitles) {
                    if (KnowledgeSimilarityUtils.isDuplicate(candidateSummary, existingTitle, dedupThreshold)) {
                        log.info("inbox-dedup hit: candidateId={}, similarity >= {}", candidate.getId(), dedupThreshold);
                        return;
                    }
                }
            } catch (Exception ex) {
                log.warn("inbox-dedup check failed, proceeding with write: {}", ex.getMessage());
            }
        }

        String topic = slug(Objects.toString(candidate.getIntent(), "") + "-" + Objects.toString(candidate.getProblem(), ""));
        if (topic.isBlank()) {
            topic = "reinforced";
        }
        String filename = topic + "-" + candidate.getId().substring(0, Math.min(8, candidate.getId().length()))
                + "-" + LocalDateTime.now().format(TS_FORMAT) + ".md";
        Path target = dir.resolve(filename);

        double avgScore = group.stream()
                .map(WikiFeedbackEvent::getEventScore)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0d);

        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("title: ").append(safe(candidate.getAbstractSummary(), candidate.getRawQuestion(), "reinforced-knowledge")).append('\n');
        sb.append("source: reinforcement-sweep\n");
        sb.append("candidate_id: ").append(candidate.getId()).append('\n');
        sb.append("stage: ").append(safe(candidate.getStage(), "unknown")).append('\n');
        sb.append("intent: ").append(safe(candidate.getIntent(), "unknown")).append('\n');
        sb.append("problem: ").append(safe(candidate.getProblem(), "unknown")).append('\n');
        sb.append("feedback_count: ").append(group.size()).append('\n');
        sb.append("avg_score: ").append(String.format(Locale.ROOT, "%.3f", avgScore)).append('\n');
        sb.append("generated_at: ").append(LocalDateTime.now()).append('\n');
        sb.append("---\n\n");
        sb.append("# ").append(safe(candidate.getAbstractSummary(), "高分知识沉淀")).append("\n\n");
        if (candidate.getRawQuestion() != null && !candidate.getRawQuestion().isBlank()) {
            sb.append("## 原始问题\n\n").append(candidate.getRawQuestion().trim()).append("\n\n");
        }
        sb.append("## 参考回答\n\n").append(candidate.getRawAnswer().trim()).append('\n');

        Files.writeString(target, sb.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        log.info("Wrote reinforcement inbox file: {}", target);
    }

    /**
     * Read the 'title' field from YAML front matter of all .md files in the inbox dir.
     */
    private List<String> readInboxTitles(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        List<String> titles = new ArrayList<>();
        try (var stream = Files.list(dir)) {
            List<Path> mdFiles = stream
                    .filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(".md"))
                    .toList();
            for (Path file : mdFiles) {
                try {
                    String content = Files.readString(file, StandardCharsets.UTF_8);
                    String title = extractFrontMatterTitle(content);
                    if (title != null && !title.isBlank()) {
                        titles.add(title);
                    }
                } catch (IOException ignored) {
                    // skip unreadable files
                }
            }
        }
        return titles;
    }

    private static String extractFrontMatterTitle(String content) {
        if (!content.startsWith("---")) {
            return null;
        }
        int endIdx = content.indexOf("\n---", 3);
        if (endIdx < 0) {
            return null;
        }
        String frontMatter = content.substring(3, endIdx);
        for (String line : frontMatter.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.toLowerCase(Locale.ROOT).startsWith("title:")) {
                return trimmed.substring(6).trim();
            }
        }
        return null;
    }

    private static String slug(String raw) {
        if (raw == null) return "";
        String cleaned = raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9一-龥]+", "-");
        cleaned = cleaned.replaceAll("^-+|-+$", "");
        if (cleaned.length() > 40) cleaned = cleaned.substring(0, 40);
        return cleaned;
    }

    private static String safe(String... candidates) {
        for (String c : candidates) {
            if (c != null && !c.isBlank()) {
                String trimmed = c.trim().replaceAll("\\s+", " ");
                return trimmed.length() > 120 ? trimmed.substring(0, 120) : trimmed;
            }
        }
        return "";
    }
}
