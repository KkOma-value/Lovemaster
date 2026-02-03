# Design: fix-manus-request-size-and-image-download

## Problem Summary
Manus 当前将 step-by-step 的对话历史与工具输出持续累加，并在每次 `think()` 调用模型时全量发送。遇到大输出工具（读文件/抓网页/终端输出）后，后续请求会快速膨胀并触发 DashScope 的 1,000,000 输入长度限制，导致 400 错误并触发重试。实际日志显示常见链路是：先 `searchBing`（5 条结果），随后 `scrapeWebPage` 打开搜索结果页，`scrapeWebPage` 返回整页 HTML（`doc.html()`），把大量无关内容写回历史。

图片下载工具在失败时缺少可诊断信息（HTTP 状态、目标路径、是否为图片响应），导致 PDF 嵌图流程频繁缺失而难以定位。

PDF 生成在遇到非 BMP 字符（emoji 等）时可能因 iText 编码器限制抛出 `This encoder only accepts BMP codepoints`，导致生成失败并把错误写回历史，进一步放大请求体。

## Goals
- Manus 模型调用不再因请求长度超限而失败（或至少能 fail-fast 给出明确原因）。
- 大输出工具不会把全文塞回会话历史；历史里保留可用片段与可追溯引用。
- 图片下载失败时能明确告诉用户“为什么失败”以及“文件应在哪里”。
- PDF 生成对 emoji 等非 BMP 字符具备最小容错（至少不崩溃）。

## Proposed Approach
### 1) Request-size budgeting (model request budget)
- 在 `ToolCallAgent.think()` 发起 `.call()` 之前，构建一个“可发送消息列表”。
- 对 `systemPrompt` 与 `messages`（包含 user/assistant/tool messages）做字符长度估算：
  - 近似长度 = `systemPrompt.length + sum(messageText.length)`（对 tool messages 计入 responseData 的字符串表示）
- 预算阈值（默认建议）：
  - `maxRequestChars = 900_000`（留余量避免序列化/结构开销导致越界）
- 裁剪策略（最小化实现）：
  1. 永远保留最新的 user prompt 与当前 step 的 nextStepPrompt。
  2. 从最旧消息开始丢弃，直到满足预算。
  3. 若仍超预算（例如单条消息就很大），对该条消息做截断并附上“已截断/引用路径”。
- 失败策略：当裁剪后仍无法满足预算（例如必须保留的消息本身超限），则 fail-fast：
  - 返回可读错误（包含当前估算长度、预算阈值、建议：改用文件引用/降低输出）。

### 2) Tool-output budgeting (history budget)
- 对高风险工具输出统一处理：
  - `FileOperationTool.ReadFile`
  - `WebScrapingTool`
  - `TerminalOperationTool`
  - 以及其它返回任意长文本的工具
- 输出阈值（默认建议）：`maxToolOutputChars = 20_000`。
- 超过阈值时：
  - 将全文写入 `<file-save-dir>/tool-output/<timestamp>-<tool>.txt`
  - 会话历史里仅写回：
    - 前 N 字符片段（例如 5,000）
    - 文件路径指针
    - 总长度与截断提示

### 3) Image download robustness & diagnostics
- 下载函数返回值应包含：
  - `status`（成功/失败）
  - `targetPath`（绝对路径）
  - `downloadDir`
  - 失败时包含 `httpStatus`（若可得）、异常摘要、以及“可能原因”（403/非图片/超时）
- 基础校验：
  - 若响应 `Content-Type` 非 `image/*`，返回可读错误并不写入/或写入并标记无效。
  - 处理常见重定向（3xx）。

### 4) Kryo chat-memory file integrity
- `.kryo` 文件必须保持二进制序列化内容；任何文本日志必须写入单独的 `.log` 文件。
- 避免在 `TabooWordAdvisor` 中对 `default.kryo` 进行追加写入（会污染会话文件）。

### 5) PDF non-BMP sanitization (minimal)
- 在写入 PDF 前，对 `content` 做 codepoint 级过滤：默认移除/替换非 BMP（`> 0xFFFF`）字符，保持实现最小、避免额外字体依赖。
- 在工具返回信息中提示“已替换/移除 emoji”，避免用户困惑。

## Risks
- 字符预算是近似：不同模型/SDK 可能存在额外结构开销。通过保守阈值（900k）降低风险。
- 裁剪/截断可能降低上下文完整性：需要在“继续执行”与“提示用户”之间做取舍（见 open questions）。

## Telemetry / Diagnostics
- 当触发预算裁剪时输出一条 info 日志：原始长度、裁剪后长度、丢弃消息数、是否截断。
- 当工具输出被外部化时输出一条 info 日志：tool 名称、原始长度、输出文件路径。
