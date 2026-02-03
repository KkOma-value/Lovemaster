## ADDED Requirements

### Requirement: Manus tool calling executes reliably
系统 SHALL 在 Manus 模式下正确执行模型返回的工具调用，并将工具执行结果写回会话上下文，使代理可继续后续推理/行动步骤。

#### Scenario: Execute a single tool call successfully
- **GIVEN** Manus 模式已注册至少一个工具（例如 `doTerminate` 或文件读取工具）
- **WHEN** 模型返回一个 tool call
- **THEN** 系统执行对应工具并把工具结果加入会话历史
- **AND** 当前 step 返回包含工具结果的可读文本

#### Scenario: Tool call references an unknown tool
- **GIVEN** 模型返回的 tool name 不存在于当前注册工具集合
- **WHEN** 代理执行该 tool call
- **THEN** 系统返回可读错误（包含 tool name）
- **AND** 代理不会因为未捕获异常而崩溃

#### Scenario: conversation history shape differs
- **GIVEN** 工具执行返回的会话历史末尾不是 `ToolResponseMessage`
- **WHEN** 代理聚合工具执行结果
- **THEN** 系统不会发生类型转换异常
- **AND** 仍能从执行结果中提取到工具响应并输出

### Requirement: Optional external tools degrade gracefully
当外部依赖配置缺失（例如 `search-api.api-key` 为空、邮件账号未配置）时，系统 SHALL 降级对应工具能力，但不影响其它工具的注册与执行。

#### Scenario: Missing search api key
- **GIVEN** `search-api.api-key` 为空或未配置
- **WHEN** 代理尝试使用 WebSearch 工具
- **THEN** 系统返回明确的“未配置”提示或不注册该工具
- **AND** 其它工具调用仍可正常执行
