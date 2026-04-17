-- T15/T16: 创建 chat_run_events 事件记录表
-- 变更: crush-probability-analysis
-- 日期: 2026-04-18
-- 说明: 记录每个 chat_run 的详细事件流，用于调试、审计和事件回放。
--        与 chat_runs 行内记录互补，chat_run_events 保存完整事件载荷（如概率分析 JSON）。

CREATE TABLE IF NOT EXISTS chat_run_events (
    id VARCHAR(36) PRIMARY KEY,
    run_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    content TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 索引：按 runId 快速查询事件列表
CREATE INDEX IF NOT EXISTS idx_chat_run_events_run_id ON chat_run_events(run_id);

-- 索引：按 runId + eventType 查询特定类型事件
CREATE INDEX IF NOT EXISTS idx_chat_run_events_run_id_type ON chat_run_events(run_id, event_type);

-- 添加注释
COMMENT ON TABLE chat_run_events IS '聊天运行事件记录表，保存每个 run 的详细事件流';
COMMENT ON COLUMN chat_run_events.run_id IS '关联的 chat_runs.id';
COMMENT ON COLUMN chat_run_events.event_type IS '事件类型（probability_result 等）';
COMMENT ON COLUMN chat_run_events.content IS '事件内容（JSON 或文本）';
