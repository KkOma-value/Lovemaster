# Change: Fix Manus DashScope function calling so tools are invoked

## Why
当前 Manus 智能体在运行时会持续输出“思考完成 - 无需行动”，最终到达最大步骤数（默认 20）而结束。

从日志可确认：
- 工具本身已注册成功（例如 `WriteFile`, `generatePDF`, `doTerminate` 等），并且工具数量非 0。
- 但模型返回的 `AssistantMessage.getToolCalls()` 为空，因此 `ToolCallAgent.think()` 判定“无需行动”，不会进入 `act()` 执行工具。

这会导致代理表现为“不会自己调用安排好的工具”，与 Manus 的自主执行预期冲突。

## What Changes
- **确保 DashScope 返回结构化 tool calls（方案一）**
  - 在 Manus 的一次“思考”调用中，确保工具定义以 DashScope 支持的方式注入到模型上下文，使其返回 `AssistantMessage.ToolCall` 列表（而非仅在文本中输出类似 `call ToolName { ... }`）。
  - 目标是让 `assistantMessage.getToolCalls().size() > 0` 成为常态，从而触发 `act()`。

- **改进 Manus 在“无工具调用”时的失败模式（最小化但更可诊断）**
  - 当连续出现“0 个工具调用”时，不应继续空转到 maxSteps；应尽早输出可读诊断信息并结束（或进入明确的错误状态）。
  - 这不会改变自主执行原则，只是避免用户看到 20 次“无需行动”。

- **不引入文本解析兜底**
  - 本变更以“让 DashScope 正常 function calling”为主，不实现 `call ... {}` 文本解析（除非后续另开 change）。

## Impact
- Affected specs: `manus-agent`
- Expected affected code (apply stage 才会改)：
  - `src/main/java/org/example/springai_learn/agent/ToolCallAgent.java`（工具定义注入方式、无工具调用时的退出策略）
  - `src/main/java/org/example/springai_learn/agent/KkomaManus.java`（如需补充模型/提示约束以触发 function calling）
  - `src/main/resources/application-*.yml`（如需补充 DashScope function calling 的配置项）

## Out of Scope
- 不新增工具本身。
- 不实现文本 `call ToolName { ... }` 解析兜底（方案二）。
- 不调整前端交互形态（仍走现有 SSE 输出）。

## Open Questions
- DashScope 在 `spring-ai-alibaba-starter 1.0.0-M6.1` 下触发 function calling 的最小必要条件是什么（模型名/参数/ChatOptions 注入方式）？
- 当前工具名称包含首字母大写（如 `WriteFile`），是否会显著影响 DashScope 的函数选择；是否需要在不破坏兼容性的前提下提供别名或规范化？
