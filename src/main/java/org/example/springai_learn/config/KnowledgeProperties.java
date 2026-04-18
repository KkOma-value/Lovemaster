package org.example.springai_learn.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.knowledge")
public class KnowledgeProperties {

    private Fanout fanout = new Fanout();
    private Wiki wiki = new Wiki();
    private Sink sink = new Sink();
    private Cache cache = new Cache();
    private Rerank rerank = new Rerank();
    private Sanitizer sanitizer = new Sanitizer();
    private Feedback feedback = new Feedback();
    private Abstractor abstractor = new Abstractor();
    private Review review = new Review();
    private Strategy strategy = new Strategy();

    @Data
    public static class Fanout {
        private int totalTimeoutMs = 800;
    }

    @Data
    public static class Wiki {
        private boolean enabled = true;
        private String root = "knowledge/wiki";
        private boolean hotReload = true;
        private int hotReloadDebounceMs = 500;
        private Retrieval retrieval = new Retrieval();
        private Graph graph = new Graph();
    }

    @Data
    public static class Retrieval {
        private int topN = 3;
        private double titleBoost = 2.0;
        private double scoreThreshold = 0.6;
        private int maxCharsPerPage = 400;
        private int totalBudgetChars = 2000;
        private int timeoutMs = 200;
    }

    @Data
    public static class Graph {
        private int expandHops = 1;
        private double expansionWeight = 0.5;
    }

    @Data
    public static class Sink {
        private double candidateKikoThreshold = 0.82;
        private int repeatHitThreshold = 3;
        private String topicSchemaPath = "knowledge/wiki/topic-schema.yml";
        private String unknownLabel = "unknown";
    }

    @Data
    public static class Cache {
        private boolean enabled = true;
        private int ttlSeconds = 300;
        private long maximumSize = 2_000L;
    }

    @Data
    public static class Rerank {
        private boolean enabled = true;
        private double dedupThreshold = 0.92;
        private int topK = 4;
        private int timeoutMs = 300;
        private int maxSegments = 12;
    }

    @Data
    public static class Sanitizer {
        private boolean enabled = true;
        private int maxContentChars = 4000;
    }

    @Data
    public static class Feedback {
        private boolean enabled = true;
        private double helpfulThreshold = 0.8;
        private int minRepeat = 2;
        private String inboxDir = "knowledge/wiki/inbox";
        private String cron = "0 0 */6 * * *";
        private int batchSize = 200;
    }

    @Data
    public static class Abstractor {
        private boolean enabled = true;
        private int maxInputChars = 4000;
        private int maxSummaryChars = 280;
    }

    @Data
    public static class Review {
        private String topicsDir = "knowledge/wiki/topics";
        private double dedupThreshold = 0.85;
        private int listPageSize = 20;
    }

    @Data
    public static class Strategy {
        private boolean enabled = true;
        private String cron = "0 30 */6 * * *";
        private int lookbackDays = 14;
        private int minSamples = 3;
        private int topK = 20;
        private int grayPercent = 10;
    }
}
