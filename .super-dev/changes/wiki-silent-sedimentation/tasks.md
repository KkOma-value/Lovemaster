# Tasks: Wiki Silent Sedimentation

> Order: 前端先行 → 后端 → 配置 → 测试 → 质量门禁
> 每个任务完成后自检：build 无错、lint 无 error、与文档一致。

## Phase 1 — 前端拆除与埋点（先行，可独立提交）

### T1.1 ActionBar 拆除书签按钮 ⭐ 入口
- 文件：`springai-front-react/src/components/Chat/ActionBar.jsx`
- 删除 `BookmarkPlus, BookmarkCheck` import
- 删除 `createKnowledgeCandidate` import
- 删除 `PERSISTED_WIKI_STATES` 常量
- 删除 props `wikiStatus`
- 删除 `localWikiStatus` state
- 删除 `effectiveWikiStatus` 派生
- 删除 `handleSaveToWiki` 方法
- 删除整块"提交到知识蒸馏"按钮 JSX
- 验证：渲染只剩 复制/👍/👎 三按钮，DOM 中不再有 `aria-label="提交到知识蒸馏"` 节点

### T1.2 MessageBubble / 调用方清理
- 文件：`springai-front-react/src/components/Chat/MessageBubble.jsx`（或同等位置）
- 删除传给 ActionBar 的 `wikiStatus` prop
- 全仓 grep `wikiStatus` 删除残留读写点
- 验证：lint 通过、无 unused var 警告

### T1.3 ChatRuntime 清理 wikiStatus 字段
- 文件：`springai-front-react/src/contexts/ChatRuntimeContext.jsx`（或对应 hook 文件）
- `updateMessage` 调用点中移除 `wikiStatus` 写入
- 消息 schema/默认值中移除 `wikiStatus`
- 验证：旧消息记录在前端反序列化不报错（兼容）

### T1.4 删除 manual 提交调用入口
- 文件：`springai-front-react/src/services/chatApi.js`（或同等位置）
- 保留 `createKnowledgeCandidate` 函数（后端兼容期不删）但移除 ActionBar 调用
- 在该函数加注释 `// deprecated: silent sedimentation 方案不再使用`

### T1.5 文案清单 grep 净化
- 全仓 grep 以下字符串确认 C 端代码完全移除：
  - "提交到知识蒸馏"
  - "知识贡献中"
  - "反馈积累中"
  - "已入知识库"
- Admin 后台保留（运营内部可见）
- 验证：`grep -r "知识贡献中" springai-front-react/src` 无结果

### T1.6 复制信号埋点
- 文件：`ActionBar.jsx`
- `handleCopy` 内调用 `createFeedbackEvent(messageId, chatId, runId, 'copy', '', 1.0, {})`
- `try/catch` 静默失败
- 验证：点复制后 Network 面板看到 POST `/api/ai/feedback/event`，eventType=copy

### T1.7 dwell 信号埋点
- 文件：`MessageBubble.jsx`
- 仅对助手消息启用 `IntersectionObserver`
- 可视 ≥ 3 秒 + `document.visibilityState === 'visible'` + 不在文档隐藏 → 发 `dwell` 事件，eventScore=停留秒数
- 同消息单次会话只上报一次（`useRef` 防重）
- 卸载时 `observer.disconnect()`
- 验证：滚动到消息停留 3s 后看到 `dwell` 事件

### T1.8 follow_up 信号埋点
- 文件：`ChatRuntimeContext` 或消息提交逻辑
- 用户提交新消息时，若上一条助手消息时间戳 ≤ 5 分钟前 → 上报 `follow_up`，关联上一条助手 messageId
- 验证：连续两轮对话时上报

### T1.9 quote 信号埋点
- 文件：消息提交逻辑
- 用户消息与上一条助手消息做最长公共子串（≥ 8 字符）匹配
- 命中则上报 `quote`，eventScore=重合长度/助手消息长度
- 验证：复述命中后看到 `quote` 事件

### T1.10 session_retention 信号埋点
- 文件：会话切换/关闭逻辑
- 离开会话时上报 `session_retention`，eventScore=停留分钟数
- 用 `navigator.sendBeacon` 或 `fetch keepalive: true`
- 验证：切走会话时上报，关闭页面也能送达

### T1.11 return_visit 信号埋点
- 文件：会话打开逻辑
- 进入历史 chatId（非新建）且距上次活跃 ≥ 1 天 → 上报 `return_visit`
- 验证：跨日打开历史会话时上报

### T1.12 前端构建与 lint
- `npm run lint` 必须通过
- `npm run build` 零错误
- 浏览器手动验证：发起一轮对话 → 复制 → 滚动 → 追问，Network 面板看到对应事件

## Phase 2 — 后端

### T2.1 KnowledgeProperties 扩展
- 文件：`KnowledgeProperties.java`
- 新增 `AutoDistill` 内类（enabled / cron / minContentLength / lookbackHours）
- `AutoApproval` 新增 `minAggregatedScore` (默认 1.5) / `negativeVeto` (默认 1) / `signalWeights` (Map<String, Double>)
- 旧字段加 `@Deprecated`，默认 `positiveEventTypes = List.of()` 退出兼容回退
- 验证：Spring Boot 启动配置加载无错

### T2.2 application.yml 默认值
- 文件：`src/main/resources/application.yml`
- 追加 `app.knowledge.auto-distill.*` + `auto-approval.signal-weights.*` + `min-aggregated-score` + `negative-veto`
- 验证：本地启动读取生效

### T2.3 SignalAggregator 实现
- 文件：`org.example.springai_learn.ai.service.SignalAggregator`（新）
- 入参：`WikiCandidate`
- 出参：`AggregatedScore { double score, int negativeCount, Map<String,Integer> breakdown }`
- 实现：根据 `(sourceChatId, sourceRunId)` + 候选关联消息时间窗 拉取 `wiki_feedback_events`，按 eventType 计数 × 权重
- 单元测试：覆盖各 eventType 单独存在、组合存在、负向否决三种场景
- 验证：`mvn test -Dtest=SignalAggregatorTest` 通过

### T2.4 KnowledgeAutoApprovalJob 改造
- 文件：`KnowledgeAutoApprovalJob.java`
- 注入 `SignalAggregator`
- 替换升级判定为：
  ```
  AggregatedScore agg = signalAggregator.score(candidate);
  if (agg.score() >= cfg.minAggregatedScore && agg.negativeCount() < cfg.negativeVeto) approve(...)
  else if (daysSinceCreation >= cfg.staleDays && agg.score() == 0) reject(...)
  else if (daysSinceCreation >= cfg.unknownTopicDays) markUnknownTopic(...)
  ```
- 日志包含 `aggregatedScore` 和 `breakdown`
- 单元测试：阈值边界 / 否决 / 冷数据 / 未知主题
- 验证：`mvn test -Dtest=KnowledgeAutoApprovalJobTest` 通过

### T2.5 ConversationDistillJob 实现
- 文件：`org.example.springai_learn.ai.service.ConversationDistillJob`（新）
- `@Scheduled(cron = "${app.knowledge.auto-distill.cron:0 */15 * * * *}")`
- 扫描 `lookbackHours` 内尚未产出候选的助手消息（按 messageId 去重）
- 过滤条件：长度 ≥ `minContentLength`、可识别 intent/problem（复用 `WikiKnowledgeService` 现有抽取）
- 创建 `WikiCandidate(source="auto_distill", status="pending_review", triggerScore=0.0)`
- metrics 上报 `distilled` 计数
- 单元测试：基础提取、去重、过滤无效消息
- 验证：`mvn test -Dtest=ConversationDistillJobTest` 通过

### T2.6 KnowledgeFeedbackService 兼容新 eventType
- 文件：`KnowledgeFeedbackService.java`
- 验证 `normalizeEventType` 对新类型（copy/dwell/follow_up/quote/session_retention/return_visit/llm_judge_positive）正常入库
- 不需要 schema 变更（已是字符串）
- 单元测试：`mvn test -Dtest=KnowledgeFeedbackServiceTest` 通过

### T2.7 隐私条款追加
- 文件：用户协议/隐私政策对应 markdown 或前端静态文本（待确认位置）
- 追加段落（见 UIUX 文档）
- 更新条款版本号

## Phase 3 — 集成与质量门禁

### T3.1 前后端联调
- 启动 Spring Boot + React，发起完整对话
- 确认所有信号事件正确入库 `wiki_feedback_events`
- 等 `ConversationDistillJob` 跑一轮，检查 `wiki_candidates` 表是否产出 `auto_distill` 行
- 等 `KnowledgeAutoApprovalJob` 跑一轮，看日志中 `aggregatedScore` 输出

### T3.2 回归测试
- 全量 `mvn test`
- 全量 `npm run lint && npm run build`
- 手动场景：thumbs_down → 候选不应被批准
- 手动场景：仅 copy 一次 → score=0.6 < 1.5 → 不批准
- 手动场景：copy + follow_up + return_visit → score=0.6+0.4+0.7=1.7 ≥ 1.5 → 批准

### T3.3 文档与 README
- README 同步删除"点击书签提交知识"等过时描述
- 更新 graphify 章节说明新方案

### T3.4 quality gate
- 运行 `super-dev quality`（如可用）
- 生成 `output/Lovemaster-quality-gate.md` 更新

## Phase 4 — 交付前

### T4.1 release readiness
- 提交、PR description 引用本 spec
- proof-pack 包含：前端截图（3 按钮 ActionBar）、后端日志样本（auto_distill + aggregatedScore）、测试报告

## 自检清单（每个 PR 提交前）

- [ ] ActionBar 仅 3 按钮，DOM 无书签节点
- [ ] grep `知识贡献中 / 已入知识库 / 提交到知识蒸馏` 在 `springai-front-react/src` 无结果
- [ ] 信号埋点失败不弹错、不阻塞主流程
- [ ] `mvn test` 通过
- [ ] `npm run lint && npm run build` 通过
- [ ] 无 emoji 用作图标
- [ ] 颜色全部走 token，无硬编码 hex
