package org.example.springai_learn.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.example.springai_learn.config.DifyProperties;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DifyRetrieveRequest(
        String query,
        RetrievalModel retrieval_model
) {

    public static DifyRetrieveRequest from(String query, DifyProperties properties) {
        DifyProperties.Retrieve retrieve = properties.getRetrieve();
        return new DifyRetrieveRequest(
                query,
                new RetrievalModel(
                        retrieve.getSearchMethod(),
                        retrieve.isRerankingEnable(),
                        retrieve.getWeights(),
                        retrieve.getTopK(),
                        retrieve.isScoreThresholdEnabled(),
                        retrieve.getScoreThreshold()
                )
        );
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RetrievalModel(
            String search_method,
            boolean reranking_enable,
            Double weights,
            int top_k,
            boolean score_threshold_enabled,
            Double score_threshold
    ) {
    }
}
