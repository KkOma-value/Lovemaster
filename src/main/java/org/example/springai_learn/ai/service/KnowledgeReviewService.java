package org.example.springai_learn.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.auth.entity.WikiCandidate;
import org.example.springai_learn.auth.repository.WikiCandidateRepository;
import org.example.springai_learn.config.KnowledgeProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeReviewService {

    private final WikiCandidateRepository wikiCandidateRepository;
    private final KnowledgeProperties properties;
    private final WikiGraphSyncService graphSyncService;

    @Transactional
    public WikiCandidate approve(String candidateId, String reviewerId, String note) {
        WikiCandidate candidate = wikiCandidateRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found: " + candidateId));

        if ("approved".equals(candidate.getStatus())) {
            log.info("Candidate already approved, candidateId={}", candidateId);
            return candidate;
        }

        candidate.setStatus("approved");
        candidate.setReviewerId(reviewerId);
        if (note != null && !note.isBlank()) {
            candidate.setRejectedReason(null); // clear any prior rejection
        }

        WikiCandidate saved = wikiCandidateRepository.save(candidate);
        log.info("Candidate approved: candidateId={}, reviewerId={}", candidateId, reviewerId);

        // Write to wiki/topics/ directory
        try {
            writeToTopics(saved);
            graphSyncService.requestSync("approve-" + candidateId);
        } catch (IOException ex) {
            log.warn("Failed to write approved candidate to topics dir: candidateId={}, error={}",
                    candidateId, ex.getMessage());
        }

        return saved;
    }

    @Transactional
    public WikiCandidate reject(String candidateId, String reviewerId, String reason) {
        WikiCandidate candidate = wikiCandidateRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found: " + candidateId));

        candidate.setStatus("rejected");
        candidate.setReviewerId(reviewerId);
        candidate.setRejectedReason(reason);

        WikiCandidate saved = wikiCandidateRepository.save(candidate);
        log.info("Candidate rejected: candidateId={}, reviewerId={}, reason={}", candidateId, reviewerId, reason);
        return saved;
    }

    /**
     * Render the structured summary as Markdown and write into
     * knowledge/wiki/topics/{stage}-{intent}-{problem}.md.
     * If the file already exists, append a new section rather than overwriting.
     */
    private void writeToTopics(WikiCandidate candidate) throws IOException {
        String topicsDir = properties.getReview().getTopicsDir();
        Path dir = Path.of(topicsDir);
        if (!dir.isAbsolute()) {
            dir = Path.of(System.getProperty("user.dir")).resolve(dir);
        }
        Files.createDirectories(dir);

        String stage = safe(candidate.getStage(), "unknown");
        String intent = safe(candidate.getIntent(), "unknown");
        String problem = safe(candidate.getProblem(), "unknown");
        String filename = slug(stage + "-" + intent + "-" + problem) + ".md";
        Path target = dir.resolve(filename);

        StringBuilder sb = new StringBuilder();
        if (Files.exists(target)) {
            // Append a new section
            sb.append("\n\n");
        } else {
            // Create with front matter
            sb.append("---\n");
            sb.append("title: ").append(stage).append(" / ").append(intent).append(" / ").append(problem).append('\n');
            sb.append("stage: ").append(stage).append('\n');
            sb.append("intent: ").append(intent).append('\n');
            sb.append("problem: ").append(problem).append('\n');
            sb.append("---\n\n");
        }

        sb.append("## ").append(candidate.getId().substring(0, Math.min(8, candidate.getId().length()))).append('\n');
        sb.append("<!-- candidate_id: ").append(candidate.getId()).append(" -->\n");
        sb.append("<!-- approved_at: ").append(LocalDateTime.now()).append(" -->\n\n");

        String summary = candidate.getAbstractSummary();
        if (summary != null && !summary.isBlank()) {
            // Try to render structured JSON nicely
            if (summary.trim().startsWith("{")) {
                sb.append("```json\n").append(summary.trim()).append("\n```\n\n");
            } else {
                sb.append(summary.trim()).append("\n\n");
            }
        }

        if (candidate.getRawQuestion() != null && !candidate.getRawQuestion().isBlank()) {
            sb.append("**原始问题**: ").append(candidate.getRawQuestion().trim()).append("\n\n");
        }

        if (Files.exists(target)) {
            Files.writeString(target, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND);
            log.info("Appended to topics file: {}", target);
        } else {
            Files.writeString(target, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            log.info("Created topics file: {}", target);
        }
    }

    private static String safe(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static String slug(String raw) {
        if (raw == null) return "unknown";
        String cleaned = raw.trim().toLowerCase().replaceAll("[^a-z0-9\\u4e00-\\u9fff]+", "-");
        cleaned = cleaned.replaceAll("^-+|-+$", "");
        if (cleaned.length() > 60) cleaned = cleaned.substring(0, 60);
        return cleaned.isEmpty() ? "unknown" : cleaned;
    }
}
