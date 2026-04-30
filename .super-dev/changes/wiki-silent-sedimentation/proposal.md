# Change Proposal: Wiki Silent Sedimentation

> Branch base: `deepseek-dev`
> Spec doc: this folder
> Source docs:
>   - `output/Lovemaster-wiki-auto-prd.md` (v2.0)
>   - `output/Lovemaster-wiki-auto-architecture.md` (v2.0)
>   - `output/Lovemaster-wiki-auto-uiux.md` (v2.0)

## Why

旧机制依赖用户点 thumbs_up + 书签按钮，绝大多数用户聊完即走，导致候选长期挨饿、wiki 停止进化；同时 ActionBar 上"知识贡献中/已入知识库"等文案对 C 端是噪音。

## What

将"显式反馈触发"改为"隐式信号 + 后台自动抽取"，UI 完全静默，删除书签按钮。

## Scope

### 前端（先行）

- 删除 `ActionBar` 书签按钮 + 所有 wiki 相关状态文案
- 删除 `MessageBubble` / `useChatRuntime` 中 `wikiStatus` 字段读写
- 删除 `chatApi.createKnowledgeCandidate` 的 manual 调用入口
- 新增 6 类隐式信号埋点：copy / dwell / follow_up / quote / session_retention / return_visit（thumbs 维持）
- 所有埋点用 `try/catch` 静默失败，建议 `keepalive` 模式

### 后端

- 新增 `ConversationDistillJob`：周期扫描对话流，自动产出 `WikiCandidate(source=auto_distill)`
- 新增 `SignalAggregator`：按候选关联事件聚合加权分
- 改造 `KnowledgeAutoApprovalJob`：阈值从「≥3 thumbs_up」→「加权分 ≥ minAggregatedScore && thumbs_down 数 < negativeVeto」
- 扩展 `KnowledgeProperties.AutoApproval` 与新增 `AutoDistill` 配置组
- `KnowledgeFeedbackService` 接受新 eventType 字符串（无需新增端点）
- LLM-judge 离线评分（P1，非 blocking）

### 配置

- `application.yml` 追加 `auto-distill` + `signal-weights` 默认值
- 旧 `min-positive-feedback / positive-event-types / positive-score-threshold` 字段保留，标 `@Deprecated`，默认空集

### 隐私

- 在用户协议/隐私政策"数据使用"小节追加去标识化声明

## Out of scope

- 信号权重热配置 UI（P2）
- A/B 实验框架（P2）
- 对历史 manual 候选的批量清理（保留按新阈值自然评估）

## Risks

- **信号刷分**：恶意用户构造大量 copy 事件刷分 → 缓解：单 user 单 message 同 eventType 同窗口去重
- **dwell 误报**：用户挂机停留时长虚高 → 缓解：可见 + 焦点 + 心跳 三重判定
- **隐私边界**：抽象化提取仍可能携带用户细节 → 缓解：`WikiKnowledgeService` 复用现有 sanitizer + maxContentChars 约束
- **回归风险**：删除书签按钮后旧版客户端继续提交 manual 候选 → 后端保留接收能力，不删除

## Acceptance Criteria

1. ActionBar 渲染只剩 3 按钮（复制/👍/👎），无任何与"知识库/沉淀"相关字眼
2. C 端任意页面 grep 不到禁用文案清单中的字符串
3. `ConversationDistillJob` 启动后能在 15 分钟内为最近对话产出至少 1 条 `auto_distill` 候选（前提：满足长度/intent 过滤）
4. `KnowledgeAutoApprovalJob` 在加权分达标时正确 approve；存在 thumbs_down 时拦截
5. 信号埋点失败不影响用户主流程（断网/服务挂掉时 UI 正常）
6. 隐私条款页面可见追加段落
