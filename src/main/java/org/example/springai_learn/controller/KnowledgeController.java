package org.example.springai_learn.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.ai.dto.CandidateListItemResponse;
import org.example.springai_learn.ai.dto.KnowledgeCandidateRequest;
import org.example.springai_learn.ai.dto.KnowledgeCandidateResponse;
import org.example.springai_learn.ai.dto.KnowledgeFeedbackEventRequest;
import org.example.springai_learn.ai.dto.KnowledgeFeedbackEventResponse;
import org.example.springai_learn.ai.dto.KnowledgeReviewRequest;
import org.example.springai_learn.ai.dto.StrategyScoreResponse;
import org.example.springai_learn.ai.service.KnowledgeFeedbackService;
import org.example.springai_learn.ai.service.KnowledgeReviewService;
import org.example.springai_learn.ai.service.KnowledgeSinkService;
import org.example.springai_learn.ai.service.StrategyRankingService;
import org.example.springai_learn.auth.entity.WikiCandidate;
import org.example.springai_learn.auth.entity.WikiStrategyScore;
import org.example.springai_learn.auth.repository.WikiCandidateRepository;
import org.example.springai_learn.config.KnowledgeProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai/knowledge")
@CrossOrigin(originPatterns = "*", allowCredentials = "false")
@RequiredArgsConstructor
@Slf4j
public class KnowledgeController {

    private final KnowledgeSinkService knowledgeSinkService;
    private final KnowledgeFeedbackService knowledgeFeedbackService;
    private final KnowledgeReviewService knowledgeReviewService;
    private final StrategyRankingService strategyRankingService;
    private final WikiCandidateRepository wikiCandidateRepository;
    private final KnowledgeProperties properties;

    private String getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof String userId) {
            return userId;
        }
        return "anonymous";
    }

    // ========== Existing endpoints ==========

    @PostMapping("/candidates")
    public ResponseEntity<KnowledgeCandidateResponse> createCandidate(@RequestBody KnowledgeCandidateRequest request) {
        String userId = getCurrentUserId();
        try {
            KnowledgeCandidateResponse response = knowledgeSinkService.createCandidate(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Create knowledge candidate failed: userId={}, error={}", userId, ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping("/feedback-events")
    public ResponseEntity<KnowledgeFeedbackEventResponse> createFeedbackEvent(
            @RequestBody KnowledgeFeedbackEventRequest request
    ) {
        String userId = getCurrentUserId();
        try {
            KnowledgeFeedbackEventResponse response = knowledgeFeedbackService.createEvent(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Create feedback event failed: userId={}, error={}", userId, ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    // ========== Step 2: Review API ==========

    /**
     * GET /ai/knowledge/candidates?status=pending_review&page=0&size=20
     */
    @GetMapping("/candidates")
    public ResponseEntity<Page<CandidateListItemResponse>> listCandidates(
            @RequestParam(defaultValue = "pending_review") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int pageSize = Math.min(size, properties.getReview().getListPageSize());
        Page<WikiCandidate> candidates = wikiCandidateRepository
                .findByStatusOrderByCreatedAtDesc(status, PageRequest.of(page, pageSize));

        Page<CandidateListItemResponse> result = candidates.map(c -> new CandidateListItemResponse(
                c.getId(),
                c.getStage(),
                c.getIntent(),
                c.getProblem(),
                c.getAbstractSummary(),
                c.getTriggerType(),
                c.getTriggerScore(),
                c.getStatus(),
                c.getCreatedAt()
        ));
        return ResponseEntity.ok(result);
    }

    /**
     * POST /ai/knowledge/candidates/{id}/approve
     */
    @PostMapping("/candidates/{id}/approve")
    public ResponseEntity<Map<String, String>> approveCandidate(
            @PathVariable String id,
            @RequestBody(required = false) KnowledgeReviewRequest request
    ) {
        String reviewerId = getCurrentUserId();
        if (request != null && request.reviewerId() != null && !request.reviewerId().isBlank()) {
            reviewerId = request.reviewerId();
        }
        String note = request != null ? request.note() : null;

        try {
            WikiCandidate approved = knowledgeReviewService.approve(id, reviewerId, note);
            return ResponseEntity.ok(Map.of(
                    "id", approved.getId(),
                    "status", approved.getStatus(),
                    "message", "Candidate approved and written to wiki/topics/"
            ));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    /**
     * POST /ai/knowledge/candidates/{id}/reject
     */
    @PostMapping("/candidates/{id}/reject")
    public ResponseEntity<Map<String, String>> rejectCandidate(
            @PathVariable String id,
            @RequestBody KnowledgeReviewRequest request
    ) {
        String reviewerId = getCurrentUserId();
        if (request != null && request.reviewerId() != null && !request.reviewerId().isBlank()) {
            reviewerId = request.reviewerId();
        }
        String reason = request != null ? request.reason() : "No reason provided";

        try {
            WikiCandidate rejected = knowledgeReviewService.reject(id, reviewerId, reason);
            return ResponseEntity.ok(Map.of(
                    "id", rejected.getId(),
                    "status", rejected.getStatus(),
                    "message", "Candidate rejected"
            ));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    // ========== Step 3: Strategy Dashboard API ==========

    /**
     * GET /ai/knowledge/strategy-scores?topicKey=&limit=20
     */
    @GetMapping("/strategy-scores")
    public ResponseEntity<List<StrategyScoreResponse>> getStrategyScores(
            @RequestParam(required = false) String topicKey,
            @RequestParam(defaultValue = "20") int limit
    ) {
        List<WikiStrategyScore> scores = strategyRankingService.topStrategies(topicKey, limit);
        List<StrategyScoreResponse> result = scores.stream()
                .map(s -> new StrategyScoreResponse(
                        s.getId(),
                        s.getTopicKey(),
                        s.getStrategyId(),
                        s.getSampleCount(),
                        s.getPositiveRate(),
                        s.getContinueRate(),
                        s.getConfidence(),
                        s.getRankScore(),
                        s.isGrayEnabled(),
                        s.getComputedAt()
                ))
                .toList();
        return ResponseEntity.ok(result);
    }
}
