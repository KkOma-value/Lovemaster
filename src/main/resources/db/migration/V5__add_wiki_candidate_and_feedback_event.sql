-- Step2: 增加知识候选与反馈事件表
-- 变更: wiki-llm-knowledge
-- 日期: 2026-04-18

CREATE TABLE IF NOT EXISTS wiki_candidate (
    id VARCHAR(36) PRIMARY KEY,
    source_chat_id VARCHAR(64) NOT NULL,
    source_run_id VARCHAR(64),
    user_id VARCHAR(36) NOT NULL,
    trigger_type VARCHAR(32) NOT NULL,
    trigger_score NUMERIC(4, 3),
    raw_question TEXT,
    raw_answer TEXT NOT NULL,
    stage VARCHAR(32) NOT NULL,
    intent VARCHAR(32) NOT NULL,
    problem VARCHAR(32) NOT NULL,
    schema_version VARCHAR(16) NOT NULL,
    abstract_summary TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'pending_review',
    reviewer_id VARCHAR(36),
    rejected_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_wiki_candidate_user_created
    ON wiki_candidate(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_wiki_candidate_status_created
    ON wiki_candidate(status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_wiki_candidate_topic
    ON wiki_candidate(stage, intent, problem);

CREATE TABLE IF NOT EXISTS wiki_feedback_event (
    id VARCHAR(36) PRIMARY KEY,
    candidate_id VARCHAR(36),
    source_chat_id VARCHAR(64) NOT NULL,
    source_run_id VARCHAR(64),
    user_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    event_value VARCHAR(64),
    event_score NUMERIC(4, 3),
    meta_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_wiki_feedback_candidate_created
    ON wiki_feedback_event(candidate_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_wiki_feedback_user_created
    ON wiki_feedback_event(user_id, created_at DESC);

COMMENT ON TABLE wiki_candidate IS '知识候选池，等待蒸馏与审核合并到 wiki';
COMMENT ON TABLE wiki_feedback_event IS '候选知识相关反馈事件，Step2 只记录，Step3 再利用';
