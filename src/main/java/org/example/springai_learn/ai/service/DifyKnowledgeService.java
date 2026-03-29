package org.example.springai_learn.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.ai.dto.DifyRetrieveRequest;
import org.example.springai_learn.ai.dto.DifyRetrieveResponse;
import org.example.springai_learn.config.DifyProperties;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DifyKnowledgeService {

    private final RestClient difyRestClient;
    private final DifyProperties properties;

    public String retrieveKnowledge(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        if (!properties.isConfigured()) {
            log.warn("Dify retrieval skipped because dataset key or dataset id is missing.");
            return "";
        }

        DifyRetrieveRequest request = DifyRetrieveRequest.from(query, properties);
        int maxAttempts = Math.max(1, properties.getRetry().getMaxAttempts());
        long startNanos = System.nanoTime();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                DifyRetrieveResponse response = difyRestClient.post()
                        .uri("/datasets/{datasetId}/retrieve", properties.getDatasetId())
                        .body(request)
                        .retrieve()
                        .body(DifyRetrieveResponse.class);

                int recordCount = response == null || response.records() == null ? 0 : response.records().size();
                String formatted = format(response);
                log.info("Dify retrieval completed in {} ms, records={}, attempt={}",
                        elapsedMillis(startNanos), recordCount, attempt);
                return formatted;
            } catch (RestClientResponseException e) {
                logWarn(attempt, maxAttempts, "Dify retrieval returned HTTP " + e.getStatusCode().value());
                if (!shouldRetry(e.getStatusCode()) || attempt >= maxAttempts) {
                    break;
                }
            } catch (ResourceAccessException e) {
                logWarn(attempt, maxAttempts, "Dify retrieval timed out or network failed: " + e.getMessage());
                if (attempt >= maxAttempts) {
                    break;
                }
            } catch (RestClientException e) {
                logWarn(attempt, maxAttempts, "Dify retrieval failed: " + e.getMessage());
                if (attempt >= maxAttempts) {
                    break;
                }
            }

            if (!sleepBeforeRetry(attempt)) {
                break;
            }
        }

        log.warn("Dify retrieval exhausted after {} attempt(s); continuing without RAG context.", maxAttempts);
        return "";
    }

    String format(DifyRetrieveResponse response) {
        if (response == null || response.records() == null || response.records().isEmpty()) {
            return "";
        }
        List<String> segments = response.records().stream()
                .map(DifyRetrieveResponse.Record::segment)
                .filter(Objects::nonNull)
                .map(DifyRetrieveResponse.Segment::content)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(text -> !text.isBlank())
                .distinct()
                .toList();
        if (segments.isEmpty()) {
            return "";
        }
        return segments.stream().collect(Collectors.joining("\n---\n"));
    }

    private boolean shouldRetry(HttpStatusCode statusCode) {
        int value = statusCode.value();
        return value == 429 || value >= 500;
    }

    private boolean sleepBeforeRetry(int attempt) {
        long backoffMs = Math.max(0, properties.getRetry().getBackoffMs()) * (1L << Math.max(0, attempt - 1));
        if (backoffMs == 0) {
            return true;
        }
        try {
            Thread.sleep(backoffMs);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Dify retrieval retry interrupted.");
            return false;
        }
    }

    private void logWarn(int attempt, int maxAttempts, String message) {
        log.warn("{} (attempt {}/{})", message, attempt, maxAttempts);
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
