## Context

当前系统以页面级 SSE 作为聊天生成的唯一生命周期载体。只要页面卸载，前端就会关闭连接；即使后端业务线程继续执行，前端也没有可恢复的运行时状态或 active run 索引。

本次变更只处理运行时与后端能力，不负责任何新的 UI 视觉层设计。

## Goals / Non-Goals

- Goals:
  - 将聊天生成从页面级副作用提升为应用级 / 后端级 run
  - 允许同一 SPA 会话内切页、回首页时保持进行中状态
  - 为关闭页面后的恢复留出后端状态接口基础
- Non-Goals:
  - 不做新的 UI 样式与动效实现
  - 不做多设备同步
  - 不引入新的模型能力

## Decisions

- Decision: Introduce an explicit run lifecycle abstraction.
  - Why: “页面是否还挂着”不应该决定一次生成是否继续执行。

- Decision: Frontend state ownership moves from `ChatPage` to an application-level runtime provider.
  - Why: 路由切换会卸载页面组件，应用级 provider 才能跨路由持有进行中状态。

- Decision: Backend exposes queryable run state even before a full resumable event-log implementation.
  - Why: 先解决 active run 可见性和恢复基础，再逐步增强事件补拉。

- Decision: Keep UI work out of this change.
  - Why: 用户已明确表示界面由其自行开发，本次只交付底层能力。

## Risks / Trade-offs

- Risk: 第一阶段若仍部分依赖旧 SSE 发起逻辑，刷新/关闭页面后的“完整恢复”能力会有限。
  - Mitigation: 先提供 run 状态查询与结果恢复，后续再补事件重连。

- Risk: Frontend runtime 与现有 `useChatMessages`、`useSSEConnection` 的职责会短期重叠。
  - Mitigation: 通过 provider 统一连接 ownership，保留现有 UI 组件消费接口，减少表层改动。

- Risk: Coach 与 Love 模式流程不同，run 状态抽象过粗会丢失关键阶段信息。
  - Mitigation: 统一 run 生命周期，同时保留 mode-specific status payload。

## Migration Plan

1. 增加前端 provider 和 active run store
2. 将聊天页改为消费 provider，而不是直接持有连接
3. 增加后端 run service 和查询接口
4. 在 orchestrator 中发布 run 状态
5. 补测试并验证路由切换场景

## Open Questions

- 当前阶段是否允许同一会话存在多个 active runs，还是限定每个 chat 只有一个 active run
- 后端 run 状态持久化是复用现有 conversation/message 表，还是增加独立 run 表
