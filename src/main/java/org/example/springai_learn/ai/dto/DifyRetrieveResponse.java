package org.example.springai_learn.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DifyRetrieveResponse(
        Query query,
        List<Record> records
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Query(String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Record(
            Segment segment,
            Double score
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Segment(
            String id,
            Integer position,
            String document_id,
            String content,
            String answer,
            Integer word_count,
            Integer tokens,
            List<String> keywords
    ) {
    }
}
