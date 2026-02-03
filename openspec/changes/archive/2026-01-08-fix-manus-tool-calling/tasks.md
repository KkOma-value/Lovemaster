# Tasks: Fix Manus tool calling to recognize DashScope function calls

## Investigation
- [x] **确认 DashScope 模型 function calling 支持**
  - 阅读 `spring-ai-alibaba-starter` 文档和 DashScope API 文档，确认当前使用的模型（通过 `application.yml` 或默认配置）是否支持 function calling。
  - 检查 `DashScopeChatOptions` 的所有可用参数，寻找启用 function calling 的选项（例如 `withTools()`, `withFunctions()`, `withResultFormat()` 等）。
  - 使用 `rg` 搜索项目中是否有其他地方成功使用了 DashScope function calling，作为参考实现。
  - **结论**: `ToolCallingChatOptions.toolCallbacks()` 是正确的方式，而 `DashScopeChatOptions.withProxyToolCalls(true)` 仅禁用自动执行但不传递工具定义。

- [x] **对比 LoveApp 工具调用逻辑**
  - 检查 `LoveApp.java` 是否使用工具调用，如果是，对比其 ChatModel 配置和调用方式与 `ToolCallAgent` 的差异。
  - 确认 `LoveApp` 是否使用相同的 DashScope 模型，还是有不同的 ChatOptions 配置。
  - **结论**: `LoveApp.doChatWithTools()` 直接使用 `.tools(allTools)` 传递工具，且不使用 proxyToolCalls，工具调用正常工作。

- [x] **验证工具注册到 LLM 上下文**
  - 在 `ToolCallAgent.think()` 中添加日志，输出 `toolsForModel` 的大小和内容，确认工具确实被传递给 `ChatClient.prompt().tools()`。
  - 检查 `ChatClient.prompt().tools()` 方法是否要求特定格式的工具对象（如 `FunctionCallback` vs `ToolCallback`），确认类型匹配。
  - **结论**: 关键是通过 `ToolCallingChatOptions.toolCallbacks()` 将工具定义包含在 `Prompt` 的 ChatOptions 中，而不是只通过 `.tools()` 方法。

## Implementation
### Option A: 启用 DashScope function calling（如果支持）
- [x] **配置 ToolCallingChatOptions 启用 function calling**
  - 在 `ToolCallAgent.think()` 和 `act()` 方法中，构建 `ToolCallingChatOptions` 包含 `toolCallbacks(allToolCallbacks)` 和 `internalToolExecutionEnabled(false)`。
  - 移除 `DashScopeChatOptions.withProxyToolCalls(true)` 配置。

- [x] **更新 think() 方法传递工具定义**
  - 确保 `Prompt` 构造时使用包含工具定义的 `ToolCallingChatOptions`。
  - 移除 `.tools(toolsForModel.toArray())` 调用，改为在 ChatOptions 中传递。

### Option B: 实现文本解析兜底方案（如 DashScope 不支持标准 function calling）
- [x] **不需要** - Option A 已验证可行，DashScope 支持标准 function calling。

## Testing
- [x] **单元测试 ToolCallAgent**
  - 运行 `ToolCallAgentTest`，验证 3/3 测试通过。
  - `act_shouldNotCastLastMessageAndShouldFinishOnTerminate` ✓
  - `act_shouldReturnReadableMessageWhenNoToolResponseMessagePresent` ✓

- [ ] **集成测试 Manus 工具调用** (手动)
  - 启动主应用，发送 `/api/ai/manus/chat` 请求（如 "生成一个包含北京景点的PDF"）。
  - 验证日志输出 `KkomaManus选择了 N 个工具来使用`，其中 N > 0。
  - 验证工具被实际执行（例如 `generatePDF` 创建了文件）。

- [ ] **回归测试自主行为** (手动)
  - 确认修复后 Manus 仍然遵循"不询问用户确认"的自主执行模式。
  - 测试复杂任务（如旅行攻略生成）能完整执行到 `doTerminate`，不中断在中间步骤。

## Documentation
- [x] **记录 DashScope function calling 配置**
  - 代码注释已说明：`// 构建包含工具回调的 ChatOptions，关键：internalToolExecutionEnabled=false 让我们自己管理执行`

## Validation
- [x] **编译通过**: `mvn -q compile -DskipTests=true` 成功
- [x] **单元测试通过**: 3/3 tests passed
