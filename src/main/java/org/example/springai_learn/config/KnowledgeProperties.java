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
    private AutoApproval autoApproval = new AutoApproval();
    private GraphSync graphSync = new GraphSync();

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
        private String cron = "0 */30 * * * *";
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

    @Data
    public static class AutoApproval {
        /** 是否启用用户反馈驱动的自动审批 */
        private boolean enabled = true;
        /** 定时检查周期 (cron) */
        private String cron = "0 */10 * * * *";
        /** 自动批准所需的最少正向反馈数 */
        private int minPositiveFeedback = 3;
        /** 正向反馈事件类型列表 */
        private java.util.List<String> positiveEventTypes = java.util.List.of("thumbs_up", "candidate_submitted", "helpful");
        /** 负向反馈事件类型列表 */
        private java.util.List<String> negativeEventTypes = java.util.List.of("thumbs_down", "unhelpful");
        /** 正向反馈最低评分阈值 */
        private double positiveScoreThreshold = 0.6;
        /** 冷数据自动清理天数 (超过此天数且反馈不足的候选自动拒绝) */
        private int staleDays = 7;
        /** 候选在 pending 状态超过此天数转为 unknown_topic */
        private int unknownTopicDays = 14;
    }

    @Data
    public static class GraphSync {
        /** 是否启用 wiki 变更后自动同步 graphify 图谱 */
        private boolean enabled = true;
        /** wiki 变更后的防抖时间 (秒) */
        private int debounceSeconds = 30;
        /** graphify 命令路径 (留空则自动查找) */
        private String graphifyCommand = "graphify";
        /** graphify 输出目录 */
        private String outputDir = "graphify-out";
    }
}
