# Design: Manus DashScope function calling

## Context
Manus 采用 ReAct 循环：每步调用 `think()` 决定是否需要 `act()`。
当前实现以 `assistantMessage.getToolCalls().isEmpty()` 作为是否行动的唯一判据；当工具调用未以结构化形式出现时，会导致每步都“无需行动”，进而空转至 `maxSteps`。

## Goal
在方案一前提下（不做文本解析兜底），让 DashScope 在给定工具定义时稳定返回结构化 tool calls，从而触发工具执行链路。

## Current Flow (high level)
1. `ToolCallAgent.think()` 调用 ChatClient → 得到 `ChatResponse`。
2. 若 `assistantMessage.getToolCalls()` 为空 → 返回 false → step 输出“无需行动”。
3. 若存在 tool calls → `act()` 使用 `ToolCallingManager.executeToolCalls()` 执行并把结果写回 history。

## Hypothesis
- DashScope 可能未收到工具定义（或未以其需要的形式收到），因此不会返回结构化 tool calls。
- 另一种可能是“读取口径问题”：`ChatResponse.hasToolCalls()` 与 `assistantMessage.getToolCalls()` 的数据来源不同，导致误判。

## Proposed Approach
### A. Ensure tool definitions are injected in the way DashScope expects
- 优先沿用 Spring AI 官方建议的 tool registration / chat options 注入方式（以 M6.1 的实际实现为准）。
- 目标：让模型侧输出变成可被 Spring AI 解析为 `AssistantMessage.ToolCall` 的结构。

### B. Fail fast on repeated no-action
- 在不破坏自主执行原则的前提下，避免空转：当 N 次无 tool calls 时，输出可读诊断并结束。
- 诊断内容应包含：已注册工具数量、模型输出片段、tool call 判定结果（两种口径）。

## Non-goals
- 不实现文本 `call ToolName { ... }` 的解析与执行（该方案需要另开变更并评估解析鲁棒性与安全边界）。

## Risks
- DashScope 模型能力差异：部分模型可能不支持或对 function calling 支持不稳定。
- 工具命名风格（如 `WriteFile` 首字母大写）可能影响函数选择质量。

## Acceptance Criteria
- 对于明确需要工具的请求（如“生成上海旅行 PDF”），每步日志/响应显示 `N > 0` 的工具调用，并能实际执行工具。
- 不再出现 20 次“无需行动”空转；无 tool calls 时能快速给出诊断。
