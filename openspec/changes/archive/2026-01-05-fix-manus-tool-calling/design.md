## Context
Manus 模式（`KkomaManus` → `ToolCallAgent`）当前采用“自维护上下文 + 手动执行工具调用”的方式：
- 在 `think()` 中让模型返回 tool calls
- 在 `act()` 中通过 `ToolCallingManager.executeToolCalls(...)` 执行工具并回写会话历史

目前实现存在两类风险：
1) **工具解析/执行链路不完整**：`ToolCallingManager` 仅 `builder().build()`，未显式配置如何从 `availableTools` 解析具体工具实现，可能导致“工具找不到/无法执行”。
2) **会话历史结构假设不成立**：`act()` 假设 `conversationHistory()` 最后一条一定是 `ToolResponseMessage` 并强制 cast，容易在不同模型/框架版本/多工具场景下触发运行时异常。

## Goals / Non-Goals
- Goals:
  - Manus 模式工具调用稳定可用，失败时可读可诊断
  - 不依赖“某个模型恰好返回某种消息顺序”
  - 外部能力缺失（搜索/邮件）不影响本地工具（文件/终端/PDF 等）
- Non-Goals:
  - 不重做 Agent 架构，不引入新 UI

## Decisions
- Decision 1: **明确采用一种工具调用机制**
  - 方案 A：继续使用手动执行（保留 think/act 分离），完善 ToolCallingManager 的 resolver 配置与历史处理
  - 方案 B：完全使用 Spring AI 自动代理工具调用（简化实现），但可能削弱对“终止/多步规划”的精细控制
  - 本提案默认倾向方案 A（更贴合现有 ReAct 结构），但允许在实施阶段依据复现与框架限制切换到方案 B。

- Decision 2: **对 ToolResponseMessage 采用健壮提取策略**
  - 不再假设最后一条消息类型固定；从 `conversationHistory()` 中查找最新的 ToolResponseMessage 或直接汇总 ToolExecutionResult。

## Risks / Trade-offs
- Spring AI / DashScope 里程碑版本差异可能改变 tool call 的消息结构；需要通过测试锁定行为。
- 如果选择方案 A，需要保证 `proxyToolCalls` 等选项不会让框架自动执行导致“双重执行”。

## Migration Plan
- 先加入回归测试与更详细日志
- 再修复 ToolCallingManager 配置与 act() 的历史处理
- 最后补齐可选配置降级与文档

## Open Questions
- Manus 失败时的具体异常堆栈是什么？（工具未找到 vs cast 异常 vs 远程模型不返回 tool calls）
- DashScope 的 `proxyToolCalls` 在当前 starter 版本下的确切语义是否与注释一致？
