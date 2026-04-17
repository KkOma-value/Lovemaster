-- T16: 数据库迁移 - 为 messages 表增加 probability_json 列
-- 变更: crush-probability-analysis
-- 日期: 2026-04-18
-- 说明: 存储 AI 概率分析的结构化 JSON 结果，用于历史消息恢复时重新渲染概率卡片

ALTER TABLE messages ADD COLUMN IF NOT EXISTS probability_json TEXT NULL;

-- 添加注释说明
COMMENT ON COLUMN messages.probability_json IS '概率分析结果 JSON（ProbabilityAnalysis record 序列化），可空';
