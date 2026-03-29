# Tasks: add-multimodal-love-coach-agents

## 实现顺序：spec → 前端优先 → preview confirmation → 后端 → 联调 → 质量

---

## Phase 1: Spec / 设计落盘

- [ ] 创建 `openspec/changes/add-multimodal-love-coach-agents/`
- [ ] 创建 proposal / tasks / design / delta specs
- [ ] 运行 `openspec validate add-multimodal-love-coach-agents --strict`

## Phase 2: 前端优先实现

- [ ] 首页双模式文案升级，明确“陪聊分析”与“任务助手”
- [ ] 聊天页头部和欢迎态改成模式感知文案
- [ ] 输入区增加截图分析提示、隐私提示、模式说明
- [ ] 流式状态支持 intake / OCR / rewrite 等阶段化展示
- [ ] Coach 模式普通分析时不自动打开 ManusPanel
- [ ] Coach 模式真正进入任务态时保持现有面板行为
- [ ] 前端构建验证并产出 preview checkpoint

## Phase 3: Preview Confirmation Gate

- [ ] 向用户展示前端改动摘要并等待确认

## Phase 4: 后端实现

- [x] `AiController` 接收 `imageUrl`
- [x] 新增共享输入上下文构建（`ChatInputContext`）
- [x] 新增截图识别 / 问题重写服务（`MultimodalIntakeService`）
- [x] 新增 RAG 知识检索服务（`RagKnowledgeService`）
- [x] Love 模式接入 intake 分析 + RAG 检索（`LoveChatOrchestrator`）
- [x] Coach 模式接入 brain 决策 + RAG 检索 + 条件式工具调用（`CoachChatOrchestrator`）
- [x] imageUrl 持久化到 Supabase（`DatabaseChatMemory.setImageUrlOnLatestUserMessage`）
- [x] RAG 知识注入系统提示词（`LoveApp.doChatWithRAGContext`）

## Phase 5: 联调与质量

- [ ] 前后端联调文本输入
- [ ] 前后端联调截图输入
- [x] `mvn compile` 验证通过
- [x] 单元测试 `CoachRoutingServiceTest` 2/2 通过
- [ ] `npm run build`
