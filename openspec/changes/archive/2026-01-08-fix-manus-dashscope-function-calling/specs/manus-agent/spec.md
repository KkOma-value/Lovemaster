# manus-agent Specification Delta

## MODIFIED Requirements

### Requirement: Manus tool calling executes reliably
系统 SHALL 在 Manus 模式下，通过 DashScope 的标准 function calling 能力返回结构化 tool calls，并据此执行工具调用与写回会话上下文，使代理可以继续后续步骤。

#### Scenario: DashScope returns structured tool calls when tools are provided
- **GIVEN** Manus 注册了 N 个可用工具（N > 0）
- **WHEN** 用户请求明确需要工具执行（例如生成 PDF、读写文件、下载资源）
- **THEN** DashScope 返回的 `AssistantMessage` 包含 `ToolCall` 对象列表（`getToolCalls().size() > 0`）
- **AND** 代理进入 `act()` 执行对应工具

#### Scenario: Tool call detection uses consistent signals
- **GIVEN** 系统获得一次模型响应
- **WHEN** 系统判定是否存在工具调用
- **THEN** 系统同时记录并对齐 `ChatResponse.hasToolCalls()` 与 `AssistantMessage.getToolCalls()` 的判定结果
- **AND** 若两者不一致，系统输出可读诊断信息（便于定位适配问题）

#### Scenario: No tool calls should not cause step-looping
- **GIVEN** Manus 在某一步得到的模型响应不包含任何 tool calls
- **WHEN** 连续出现无 tool calls 的情况达到阈值 N（建议 1-2）
- **THEN** 系统输出可读诊断信息并结束执行（而不是空转直至 maxSteps）
- **AND** 诊断信息包含：已注册工具数量、模型输出摘要、tool call 判定结果
