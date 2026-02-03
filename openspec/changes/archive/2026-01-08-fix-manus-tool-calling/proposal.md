# Change: Fix Manus tool calling to recognize DashScope function calls

## Why
当前 Manus 在接收用户请求后，虽然 LLM 输出包含工具调用意图（日志显示 "call GenerateTravelItinerary {...}"），但 `KkomaManus选择了 0 个工具来使用`，导致每个步骤都是"思考完成 - 无需行动"，最终达到最大步骤数而失败。

**根本原因**：
- DashScope 模型返回的是**文本形式**的工具调用（如 `"call GenerateTravelItinerary {...}"`），而不是 OpenAI-style 的 function call 结构。
- 当前代码使用 `DashScopeChatOptions.withProxyToolCalls(true)`，期望 LLM 返回标准 `AssistantMessage.ToolCall` 对象，但实际得到的是普通文本。
- Spring AI 的 `ChatResponse.hasToolCalls()` 检查工具调用对象列表，文本形式无法被识别，因此 `toolCallList.size()` 始终为 0。

**观察到的症状**：
```
AI Response: call GenerateTravelItinerary {
  "destination": "上海",
  "days": 4,
  "include_images": true
}
KkomaManus选择了 0 个工具来使用
```

这表明 DashScope 需要显式的 function calling 配置或不同的提示策略，才能返回结构化的 function call。

## What Changes
- **调查 DashScope function calling 支持**：
  - 检查 `DashScopeChatOptions` 是否有启用 function calling 的额外选项（如 `withFunctionCallbacks()` 或特定模型参数）。
  - 确认当前使用的 DashScope 模型（如 `qwen-turbo` vs `qwen-max`）是否支持 function calling，是否需要切换模型或添加特定参数。
  
- **修复 ToolCallAgent 的工具注册逻辑**：
  - 确保工具在 `ChatClient.prompt().tools()` 中正确传递给 LLM 上下文。
  - 验证 `DashScopeChatOptions` 配置是否正确启用 function calling，或移除 `withProxyToolCalls(true)` 如果不适用。
  
- **备选方案**（如 DashScope 不支持标准 function calling）：
  - 实现文本解析逻辑：从 `AssistantMessage.getText()` 中提取工具名称和参数（例如正则匹配 `call ToolName { ... }`）。
  - 手动构造 `ToolCall` 对象并传递给 `ToolCallingManager.executeToolCalls()`。
  
- **更新 KkomaManus 系统提示**（如需要）：
  - 如果 DashScope 需要特定格式的提示才能触发 function calling，更新 `SYSTEM_PROMPT` 包含函数描述或示例。

## Impact
- Affected specs: manus-agent
- Affected code (expected):
  - `src/main/java/org/example/springai_learn/agent/ToolCallAgent.java`（工具注册和调用逻辑）
  - `src/main/java/org/example/springai_learn/agent/KkomaManus.java`（DashScope ChatOptions 配置）
  - 可能需要添加 `DashScopeToolCallParser.java`（如需文本解析方案）
  - tests

## Out of Scope
- 不改变前端交互逻辑。
- 不影响 LoveApp 的工具调用行为（LoveApp 如果使用不同模型或配置则不受影响）。
- 不新增工具本身，仅修复现有工具的调用机制。
