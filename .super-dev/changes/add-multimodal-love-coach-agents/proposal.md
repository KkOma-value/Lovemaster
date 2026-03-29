# Proposal: 多 Agent 截图陪聊 + Coach 决策分流

## 变更 ID
`add-multimodal-love-coach-agents`

## 状态
`approved` — 用户已确认 research / prd / architecture / uiux

## 概述
为 Lovemaster 增加“聊天截图识别 + 问题重写 + Love / Coach 双模式分流”能力。Love 模式优先做陪聊分析和回复建议，Coach 模式先理解问题，再决定是否进入 Manus 工具执行。

## 确认决策
1. **第一阶段架构**: 采用 IntakeRewriteAgent + BrainAgent + ToolBrokerAgent 的增量式三层结构
2. **框架策略**: 第一阶段不整体迁移到新版 Graph / Supervisor
3. **前端优先**: 先做模式文案、截图输入体验、分阶段状态和 Coach 面板打开策略
4. **工具路由**: Coach 模式只有在确实需要执行任务时才进入 Manus
5. **流程门禁**: 前端实现完成后必须停在 preview confirmation，再继续后端

## 参考文档
- `output/lovemaster-multi-agent-research.md`
- `output/lovemaster-multi-agent-prd.md`
- `output/lovemaster-multi-agent-architecture.md`
- `output/lovemaster-multi-agent-uiux.md`
