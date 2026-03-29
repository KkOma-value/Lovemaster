package org.example.springai_learn.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "dify.api")
public class DifyProperties {

    private String baseUrl = "https://api.dify.ai/v1";
    private String datasetKey;
    private String datasetId;
    private Retrieve retrieve = new Retrieve();
    private Timeout timeout = new Timeout();
    private Retry retry = new Retry();

    public boolean isConfigured() {
        return hasText(datasetKey) && hasText(datasetId);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Data
    public static class Retrieve {
        private String searchMethod = "hybrid_search";
        private boolean rerankingEnable = false;
        private Double weights = 0.7;
        private int topK = 4;
        private boolean scoreThresholdEnabled = false;
        private Double scoreThreshold;
    }

    @Data
    public static class Timeout {
        private int connectMs = 5000;
        private int readMs = 10000;
    }

    @Data
    public static class Retry {
        private int maxAttempts = 2;
        private int backoffMs = 1000;
    }
}
