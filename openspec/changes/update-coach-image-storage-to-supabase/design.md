## Context
Coach 模式当前会先使用 `downloadResource` / `downloadImage` 把图片写入本地 `tmp/download`，随后 ToolsAgent 再尝试读取本地文件并补传到 Supabase Storage。这个链路有三个问题：

- 图片历史回显依赖本地文件是否还在
- 数据库存储与 SSE 渲染有两套 URL 语义，容易分叉
- 运行环境迁移后，本地路径失效，历史会话图片不可用

用户已经明确要求采用 `Supabase Storage + DB 元数据` 方案，前端直接使用云端链接渲染。

## Goals / Non-Goals
- Goals:
  - Coach 图片资产以 Supabase Storage 为唯一持久化来源
  - `conversation_images` 只保存元数据与可渲染 URL
  - 页面刷新或重新进入会话后，图片可直接从数据库回放并展示
  - 失败场景可诊断，不产生脏数据
- Non-Goals:
  - 不改造普通聊天截图上传链路
  - 不把图片二进制直接写入 PostgreSQL
  - 不在这次变更中处理历史旧数据回填

## Decisions
- Decision: Coach 图片下载改为“抓取远端字节流 -> 上传 Supabase Storage -> 写入元数据 -> SSE 推送 public URL”
  - Why: 这样前端、数据库、历史恢复三者都围绕同一个云端 URL 工作，不再依赖本地文件路径

- Decision: `conversation_images` 只使用 `public_url`、`storage_path`、`source_url`
  - Why: 当前 `url` / `file_path` 语义是旧的本地方案残留，继续混用会让接口和实体长期不一致

- Decision: 搜索预览 URL 仍可通过 SSE 直接展示，但只有真正上传成功的图片才持久化
  - Why: 这样既保留工具执行中的即时反馈，又避免把临时预览当成会话资产

- Decision: 若 Supabase Storage 未配置或上传失败，Coach 图片工具返回可读错误，不再把本地路径作为持久化回退
  - Why: 本次变更的目标就是去掉本地文件依赖，静默回退会破坏这一点

## Risks / Trade-offs
- 外部图片抓取和 Supabase 上传都走网络，单次工具耗时会增加
  - Mitigation: 复用现有超时控制，并在日志和工具结果中输出明确失败原因

- MCP `downloadImage` 与本地 `downloadResource` 现在默认返回本地路径文本
  - Mitigation: 由 ToolsAgent 接管“图片工件落地”语义，解析结果中的源 URL 或扩展工具返回，以便直接上传云端

- 历史数据表可能仍有旧字段或旧数据
  - Mitigation: 本次先保证新链路一致；若需要清理旧列，作为后续数据库迁移处理

## Migration Plan
1. 对齐 `ConversationImage` 实体、查询接口和前端消费字段
2. 增加基于远端 URL 的 Supabase 上传入口
3. 重写 ToolsAgent 图片工件处理为云端优先
4. 更新前端历史加载与预览逻辑
5. 运行构建与手工回归

## Open Questions
- 是否需要在会话删除时同步删除 Supabase Storage 对象，而不只是删数据库记录
- 是否要为私有 bucket 改成签名 URL；当前默认沿用 public bucket
