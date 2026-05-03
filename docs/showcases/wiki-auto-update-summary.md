# Wiki 全自动反馈驱动更新系统 - 开发总结

> Lovemaster | 2026-04-25 | Super Dev 全自动流水线

---

## 一、背景与目标

### 旧机制
原有的 Wiki 知识库更新需要**人工审批**：

```
用户点击"保存到Wiki" → Candidate(pending_review) → 管理员审核 → 通过/驳回 → topics/
                                                           ↑
                                                   人工瓶颈，无法规模化
```

- 知识入库需要管理员逐条审核
- 反馈信号（点赞/点踩）仅用于评分统计，不直接驱动入库
- 图谱 (graphify-out/) 需手动运行 `graphify update .`
- 反馈处理周期长达 6 小时

### 新机制
**用户反馈信号直接驱动 Wiki 进化**，零人工介入：

```
用户反馈 (点赞/保存/继续对话) → 信号聚合 → 自动审批/清理 → topics/ → 图谱自动同步
```

---

## 二、核心架构变更

### 数据流对比

| 环节 | 旧 | 新 |
|------|-----|-----|
| **知识入库** | 管理员手工 Y/N | 达到 3 个正向反馈自动入库 |
| **冷数据清理** | 无 | >=7 天无反馈自动清理 |
| **反馈处理** | 每 6 小时 | 每 30 分钟 |
| **图谱同步** | 手动 `graphify update .` | 文件变更后自动同步 |
| **候选过期** | 无处理 | >=14 天标记 unknown_topic |
| **审批界面** | 审核操作台 | 观察仪表盘 |

### 新增组件

```
┌─────────────────────────────────────────────────────┐
│              KnowledgeAutoApprovalJob                │
│  "反馈驱动自动审批引擎"                                │
│  · 每 10 分钟扫描 pending_review 候选                 │
│  · 聚合 thumbs_up / candidate_submitted 事件         │
│  · >=3 正向反馈 → 自动批准 → 写入 topics/             │
│  · >7 天无反馈 → 自动拒绝                              │
│  · >14 天 → 标记 unknown_topic                       │
└─────────────────────┬───────────────────────────────┘
                      │
┌─────────────────────┴───────────────────────────────┐
│              WikiGraphSyncService                    │
│  "Wiki ↔ 图谱自动同步"                                │
│  · 30 秒防抖，合并高频触发                             │
│  · 异步执行 graphify update .                        │
│  · 120 秒超时保护                                     │
│  · 所有写入路径均触发 (approve/reinforcement)          │
└─────────────────────────────────────────────────────┘
```

---

## 三、文件变更清单

### 新增文件 (4)

| 文件 | 行数 | 职责 |
|------|------|------|
| `src/.../ai/service/KnowledgeAutoApprovalJob.java` | 117 | 反馈驱动自动审批定时任务 |
| `src/.../ai/service/WikiGraphSyncService.java` | 116 | Wiki → graphify 图谱同步服务 |
| `scripts/wiki-update.sh` | 210 | CLI 统一更新入口 (防抖+锁+索引) |
| `scripts/setup-wiki-autoupdate.sh` | 72 | 一键安装 git hooks + 首次图谱生成 |

### 修改文件 (7)

| 文件 | 变更 |
|------|------|
| `config/KnowledgeProperties.java` | +36行: AutoApproval + GraphSync 配置块, cron 6h→30min |
| `ai/service/KnowledgeReinforcementJob.java` | +6行: 注入 GraphSyncService, 写入后触发同步 |
| `ai/service/KnowledgeReviewService.java` | +2行: 注入 GraphSyncService, 审批后触发同步 |
| `ai/service/KnowledgeMetrics.java` | +27行: autoApproved/autoRejected/unknownTopic 指标 |
| `auth/repository/WikiFeedbackEventRepository.java` | +2行: findByCandidateId 查询方法 |
| `springai-front-react/.../ActionBar.jsx` | 文案更新: 用户反馈驱动语义 |
| `springai-front-react/.../KnowledgeReviewPage.jsx` | 重写: 审核台→观察台, 连接真实API |
| `.claude/settings.local.json` | +12行: PostToolUse hook (Edit/Write后触发wiki更新) |

---

## 四、关键配置

```yaml
app.knowledge:
  auto-approval:
    enabled: true
    cron: "0 */10 * * * *"           # 每 10 分钟检查一次
    min-positive-feedback: 3         # 自动批准需要的正向反馈数
    positive-score-threshold: 0.6    # 正向反馈最低评分
    stale-days: 7                    # 冷数据清理天数
    unknown-topic-days: 14           # 长期未处理标记天数
  graph-sync:
    enabled: true
    debounce-seconds: 30             # 图谱同步防抖(秒)
    graphify-command: "graphify"     # CLI 路径
  feedback:
    cron: "0 */30 * * * *"           # 从 6h 缩短到 30min
```

---

## 五、三条自动规则

| 规则 | 条件 | 动作 |
|------|------|------|
| **自动入库** | 正向反馈 >= 3 条 | 自动 approve → 写入 `knowledge/wiki/topics/` |
| **自动清理** | 创建 >= 7 天 && 零正向反馈 | 自动 reject → 标记已清理 |
| **自动归档** | 创建 >= 14 天 && 仍 pending | 标记 `unknown_topic` 供后续分析 |

---

## 六、构建验证

| 项目 | 结果 |
|------|------|
| Maven compile (137 源文件) | BUILD SUCCESS |
| ESLint (ActionBar + KnowledgeReview) | 通过 (零错误零警告) |
| Spring Boot 兼容性 | Java 21, Spring Boot 3.4.5 |

---

## 七、与现有系统的集成

```
KnowledgeSinkService          ← 用户手动"保存到Wiki" (不变)
    ↓
WikiCandidate (pending_review)
    ↓
KnowledgeAutoApprovalJob      ← 【新增】替代人工审批
    ↓
KnowledgeReviewService        ← 仍保留 approve/reject API (兼容旧路径)
    ↓
WikiKnowledgeService          ← 热加载 .md 文件变更 (不变)
    ↓
RagKnowledgeService           ← 检索增强 (不变)
    ↓
CoachChatOrchestrator         ← 注入知识到对话 (不变)
    ↓
用户反馈 (点赞/点踩)            ← 反馈信号回到 AutoApprovalJob (完整闭环)
    ↓
KnowledgeReinforcementJob     ← 高分反馈自动沉淀 (增强)
    ↓
WikiGraphSyncService          ← 【新增】触发 graphify 图谱同步
```
