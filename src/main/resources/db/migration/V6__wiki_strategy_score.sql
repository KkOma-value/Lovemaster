-- Step 3: Strategy feedback loop - scoring table
CREATE TABLE IF NOT EXISTS wiki_strategy_score (
    id              VARCHAR(36) PRIMARY KEY,
    topic_key       VARCHAR(96)    NOT NULL,
    strategy_id     VARCHAR(64)    NOT NULL,
    sample_count    INT            NOT NULL DEFAULT 0,
    positive_rate   DECIMAL(5,4),
    continue_rate   DECIMAL(5,4),
    confidence      DECIMAL(5,4),
    rank_score      DECIMAL(6,4),
    gray_enabled    BOOLEAN        NOT NULL DEFAULT FALSE,
    computed_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_topic_strategy UNIQUE (topic_key, strategy_id)
);

CREATE INDEX IF NOT EXISTS idx_wss_topic_rank ON wiki_strategy_score (topic_key, rank_score DESC);
