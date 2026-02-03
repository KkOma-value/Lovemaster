# Change: fix-manus-request-size-and-image-download

## Why
当前 Manus（`KkomaManus`/`ToolCallAgent`）在每一步调用模型时，会将 `messageList`（包含历史对话、工具调用返回的 `ToolResponseMessage` 等）不加约束地累计并原样传给 DashScope。

在现有工具集合中，至少有以下工具可能产生“非常大”的文本输出：
- `FileOperationTool.ReadFile(...)`：直接返回完整文件内容（无长度上限）
- `WebScrapingTool`：抓取网页正文（通常可达几十 KB 以上）
- `TerminalOperationTool`：终端输出可能很长

当上述工具输出被写回会话历史并在后续 step 中继续携带时，DashScope 侧会返回 400：
`Range of input length should be [1, 1000000]`
这与“代理应可靠执行工具调用并继续后续步骤”的 manus-agent 目标冲突。

同时，项目中引入 Kryo（`FileBasedChatMemory`）只能解决“持久化存储”，并不会自动减少发送到模型的请求长度；对于 Manus 路径（当前不使用 `MessageChatMemoryAdvisor`）更是不会产生直接帮助。

另外，从现网日志可观察到一个典型的“爆量链路”：
- Step1 调用 `searchBing` 返回 5 条搜索结果（符合工具设定）
- Step2 紧接着调用 `scrapeWebPage(url)` 打开其中某个结果页面
- `scrapeWebPage` 当前实现返回 `doc.html()`（整页 HTML 源码），这会把大量 `<script>/<style>`、导航等无关内容写回会话历史
- 后续 step 继续携带该大段 HTML 调用模型，最终触发 DashScope 输入长度上限或造成响应极慢

图片下载方面：MCP 的 `downloadImage` 与本地 `downloadResource` 都基于 Hutool 的 `HttpUtil.downloadFile`，缺少对重定向/HTTP 状态/内容类型（是否真为 image）/可读诊断的处理，导致“下载失败但原因不清晰”，进而使 `generatePDF` 的 Markdown 嵌图经常缺失。

PDF 生成方面：现网日志显示 `generatePDF` 在写入包含 emoji 的文本（例如 🌟、1️⃣ 等）时，iText 可能抛出 `This encoder only accepts BMP codepoints`。
这会导致“明明 content 很短也无法生成 PDF”，并且失败后的错误会被写回会话历史，进一步放大后续请求体。

## What Changes
- 为 Manus 的模型调用引入“请求大小预算（request budget）”机制：
  - 在调用 DashScope 前对 `systemPrompt + messages` 做长度估算并裁剪/截断，确保不触发 1,000,000 长度上限。
  - 当无法在预算内满足请求时，返回可读错误并终止（避免重试风暴）。
- 为高风险工具输出引入“输出预算（tool output budget）”机制：
  - 超过阈值的 tool 输出不再全文写回历史；写回内容改为“摘要/片段 + 指针（文件路径或引用）”。
- 提升图片下载的健壮性与可诊断性：
  - 明确返回 HTTP 状态、目标路径、downloadDir。
  - 对非图片响应（HTML/JSON）给出可读错误。
  - 处理常见的重定向与超时场景。
- 修正 Kryo 记忆相关的“潜在文件污染”风险：确保纯文本事件日志不会写入 `.kryo` 二进制会话文件。

- 提升 PDF 生成对非 BMP 字符（emoji 等）的容错：
  - 在写入 PDF 之前对 `content` 做最小化净化（例如剔除/替换非 BMP codepoints），避免 iText 因字符集编码限制直接失败。

## Non-Goals
- 不引入复杂的 token 精确计数（以字符长度预算作为近似，保持实现最小化）。
- 不改变 DashScope 的重试策略与上游 SDK 行为。
- 不新增 UI/交互流程。

## Impact
- Affected specs: `manus-agent`（新增“请求大小预算/工具输出预算/下载诊断”的要求与场景）。
- Affected code (implementation stage):
  - `org.example.springai_learn.agent.ToolCallAgent`
  - `org.example.springai_learn.tools.*`（File/Web/Terminal/Download）
  - `org.example.mcpservers.server.ImageSearchTool`
  - `org.example.springai_learn.ChatMemory.FileBasedChatMemory` / `TabooWordAdvisor`（日志落盘约束）

## Open Questions (need your confirmation)
1. 你更希望“超预算时自动裁剪历史继续执行”，还是“超预算立刻失败并提示用户缩小任务/改用文件引用”？
2. 预算阈值希望是多少？建议默认：请求总长度上限 900,000 字符（给 SDK/序列化留余量），单条 tool 输出上限 20,000 字符。
3. 图片下载主要来自哪些来源（Pexels / 任意 URL / 公众号/博客图床）？不同来源对 UA/Referer 要求不同，会影响下载策略。
4. 你希望 PDF 中如何处理 emoji：直接删除、替换为占位符（例如 `?`），还是尝试保留（需要引入支持 emoji 的字体与更复杂实现）？
