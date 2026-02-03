# Tasks: Fix Manus DashScope function calling so tools are invoked

## 1. Investigation
- [x] 复核当前 Manus 运行时的关键证据
  - [x] 基于现有日志确认：工具已注册成功但 `getToolCalls()` 为 0，导致每步“无需行动”。
  - [x] 在代码中补齐 `hasToolCalls/getToolCalls` 双口径日志，便于后续直接从运行日志定位。

- [x] 确认 DashScope function calling 的正确启用方式（针对 `spring-ai-alibaba-starter 1.0.0-M6.1`）
  - [x] 通过反编译确认 `DashScopeChatOptions` 支持 `withFunctionCallbacks(...)` 与 `withProxyToolCalls(...)`。
  - [x] 明确：需要通过 `ChatClient.options(DashScopeChatOptions)` 显式注入 function callbacks。

## 2. Spec Delta
- [x] 更新 `manus-agent` 的 delta spec
  - [x] 增加/修改场景：DashScope 在 tools 被正确注入时必须返回结构化 tool calls。
  - [x] 增加/修改场景：无 tool calls 时应尽早给出诊断并停止空转。

## 3. Implementation (apply stage)
- [x] 调整 Manus think() 调用使工具定义进入模型上下文
  - [x] 使用 `DashScopeChatOptions.withFunctionCallbacks(...)` + `withProxyToolCalls(true)`，并通过 `ChatClient.options(...)` 注入。
  - [x] 在日志中同时输出 `assistantMessage.getToolCalls().size()` 与 `chatResponse.hasToolCalls()` 以避免口径误判。

- [x] 改善“无工具调用”时的行为
  - [x] 连续无 tool calls 达阈值时，输出诊断并设置状态为 FINISHED，避免空转到 20 步。

## 4. Testing
- [x] 单元测试（不依赖真实 DashScope）
  - [x] 新增 fail-fast 分支的单测，确保无工具调用时会返回可读诊断并结束。
  - [x] 保持原有 `act()` 行为测试通过。

- [ ] 手动集成测试（真实 DashScope）
  - [ ] 请求："生成一个包含上海 3 天游玩攻略的 PDF"，期望 N>0 tool calls，并最终调用 `doTerminate`。
  - [ ] 验证输出不出现 20 次“无需行动”，且工具确实执行（例如生成文件）。
  - [ ] 说明：该步骤会触发真实 DashScope 调用，建议你在确认成本后手动验证。

## 5. Validation
- [x] `openspec validate fix-manus-dashscope-function-calling --strict`
- [x] `mvn -q test`（或最小相关测试集）
