# manus-agent Specification

## Purpose
TBD - created by archiving change fix-manus-tool-calling. Update Purpose after archive.
## Requirements
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

### Requirement: Optional external tools degrade gracefully
当外部依赖配置缺失（例如 `search-api.api-key` 为空、邮件账号未配置）时，系统 SHALL 降级对应工具能力，但不影响其它工具的注册与执行。

#### Scenario: Missing search api key
- **GIVEN** `search-api.api-key` 为空或未配置
- **WHEN** 代理尝试使用 WebSearch 工具
- **THEN** 系统返回明确的“未配置”提示或不注册该工具
- **AND** 其它工具调用仍可正常执行

### Requirement: Manus file-generation tool writes provided content
系统 SHALL 确保 Manus 调用的 PDF 生成工具会将模型提供的内容写入目标 PDF 文件；当内容包含 Markdown 图片语法时，系统 SHALL 尝试从本地下载目录加载并嵌入图片。

#### Scenario: PDF tool writes provided text
- **GIVEN** 模型返回 `generatePDF` 工具调用，参数包含非空 `fileName` 与 `content`
- **WHEN** 系统执行工具
- **THEN** 生成的 PDF 文件包含传入的 `content` 文本（可在生成文件中可见）
- **AND** 工具执行结果反馈包含生成路径

#### Scenario: PDF embeds local images referenced by Markdown
- **GIVEN** `content` 中包含 Markdown 图片语法 `![](gugong.jpg)`
- **AND** 本地存在图片文件 `<projectRoot>/tmp/download/gugong.jpg`
- **WHEN** 系统执行 `generatePDF`
- **THEN** 生成的 PDF 中包含对应图片内容（图片被嵌入而不是仅显示原始文本）

#### Scenario: Markdown image missing yields readable error
- **GIVEN** `content` 中包含 Markdown 图片语法 `![](gugong.jpg)`
- **AND** `<projectRoot>/tmp/download/gugong.jpg` 不存在或不可读
- **WHEN** 系统执行 `generatePDF`
- **THEN** 系统返回可读错误（包含缺失图片文件名与期望路径）
- **AND** 系统不会静默生成缺图 PDF

### Requirement: Shared download directory for MCP image download and PDF embedding
当 Manus 使用 MCP server 下载图片并随后生成 PDF 时，系统 SHALL 确保“下载图片落盘目录”与“PDF 嵌图读取目录”一致且可配置，避免因工作目录不同导致图片无法被嵌入。

#### Scenario: MCP downloads into shared directory and PDF embeds it
- **GIVEN** 配置了共享文件根目录（例如 `app.file-save-dir`）
- **AND** MCP `downloadImage(url, fileName)` 成功写入 `<file-save-dir>/download/<fileName>`
- **WHEN** `generatePDF` 的 `content` 引用 `![](<fileName>)`
- **THEN** 生成的 PDF 中包含对应图片内容

#### Scenario: Directories are mismatched and system provides diagnostics
- **GIVEN** 未配置共享文件根目录且主应用与 MCP server 以不同工作目录启动
- **WHEN** `generatePDF` 尝试嵌入 `content` 中引用的本地图片
- **THEN** 工具返回可读错误，包含缺失图片文件名与期望路径（expected at ...）
- **AND** 返回信息包含解析后的 downloadDir，便于定位目录不一致

### Requirement: PDF generation supports runtime CJK fonts
系统 SHALL 在运行时具备生成包含中文等 CJK 字符的 PDF 能力，不得因字体依赖仅存在于测试类路径而导致生成失败或空白 PDF。

#### Scenario: Runtime PDF generation does not fail due to missing font-asian
- **GIVEN** 系统以非测试方式运行（生产运行时类路径）
- **WHEN** Manus 调用 `generatePDF` 生成包含中文字符的内容
- **THEN** PDF 生成不会抛出“字体不可识别/缺失”类异常
- **AND** 生成的 PDF 不为空白且可读

#### Scenario: Regression guard for dependency scope
- **GIVEN** 项目构建执行单元测试
- **WHEN** `pom.xml` 中将 `com.itextpdf:font-asian` 误设为 `test` scope
- **THEN** 测试应失败并提示该依赖必须在运行时可用

### Requirement: MCP image download tool is callable from Manus when configured
当 MCP 客户端被启用且 MCP server 可用时，系统 SHALL 允许 Manus 调用 MCP 服务器提供的下载图片工具，将图片保存到本地下载目录以供 PDF 嵌入。

#### Scenario: Download image via MCP then embed into PDF
- **GIVEN** MCP client 已启用并已连接到 MCP server
- **WHEN** Manus 调用 `downloadImage(url, fileName)`
- **THEN** 系统将图片保存到 `<projectRoot>/tmp/download/<fileName>`
- **AND** 随后 `generatePDF` 引用 `![](<fileName>)` 时可成功嵌入该图片

#### Scenario: MCP is not enabled or server unavailable
- **GIVEN** MCP client 未启用或 MCP server 不可达
- **WHEN** Manus 尝试调用 MCP 工具
- **THEN** 系统返回明确的“未启用/不可达”的可读错误
- **AND** 不影响其它本地工具（如 `generatePDF`、`downloadResource`）的调用

### Requirement: Manus executes autonomously without user confirmation
系统 SHALL 确保 Manus 在每个步骤中至少调用一个工具（除了最终 `doTerminate`），不输出任何询问用户确认的文本，直到任务完成或达到最大步骤数。

#### Scenario: Complex task requires multiple tool calls
- **GIVEN** 用户请求需要多步骤完成（如 "生成包含图片的上海旅行PDF"）
- **WHEN** Manus 执行任务
- **THEN** 每个步骤日志输出 `KkomaManus选择了 N 个工具来使用`，其中 N > 0
- **AND** 不输出包含 "请确认" 或 "用户确认" 的文本
- **AND** 最终以 `doTerminate` 工具调用结束，或达到最大步骤数

#### Scenario: Missing information handled autonomously
- **GIVEN** 任务参数不完整（如未指定天数、未提供具体地点等）
- **WHEN** Manus 处理任务
- **THEN** 系统基于合理默认值自主决策（如默认3天、选择热门景点）
- **AND** 直接调用工具执行，不询问用户确认
- **AND** 工具调用日志显示选择了 N 个工具（N > 0）

### Requirement: Manus model requests SHALL stay within provider input limits
系统 SHALL 在调用 DashScope 进行 chat completion 前，对待发送的 `systemPrompt + messages` 应用请求大小预算（默认 900,000 字符），避免触发 provider 侧的 `Range of input length should be [1, 1000000]` 错误。

#### Scenario: Oversized tool output does not break subsequent steps
- **GIVEN** 某次工具调用产生了较大文本输出（例如读取大文件或抓取网页）
- **AND** 工具输出会被写回会话历史用于下一步推理
- **WHEN** Manus 进入下一次 `think()` 需要调用模型
- **THEN** 系统会对请求应用预算并裁剪历史/截断过大内容，使发送请求不超过上限
- **AND** 系统输出可读诊断（例如裁剪了多少历史、是否发生截断）

#### Scenario: Mandatory message alone exceeds budget
- **GIVEN** 单条必须保留的消息（例如最新 user prompt）本身就超过预算
- **WHEN** 系统构建模型请求
- **THEN** 系统 SHALL fail-fast 并返回可读错误（包含估算长度与预算阈值）
- **AND** 系统 SHOULD 建议改用文件引用或缩小输入

### Requirement: Tool outputs written to conversation history SHALL be bounded
系统 SHALL 对可能产生大输出的工具施加输出预算（默认 20,000 字符）；当输出超过阈值时，系统 SHALL 将全文截断，并附加"[truncated]"标记和原始长度提示。

#### Scenario: ReadFile output is truncated when too large
- **GIVEN** Manus 调用 `ReadFile(fileName)`
- **AND** 文件内容长度超过输出阈值
- **WHEN** 系统返回工具执行结果
- **THEN** 返回内容被截断至阈值长度，包含 `[truncated]` 标记
- **AND** 返回信息包含完整文件路径以便后续引用

#### Scenario: TerminalOperation output is truncated when too large
- **GIVEN** Manus 调用 `executeTerminalCommand(command)`
- **AND** 命令输出长度超过输出阈值
- **WHEN** 系统返回工具执行结果
- **THEN** 返回内容被截断至阈值长度，包含 `[truncated]` 标记

### Requirement: Image download failures SHALL be diagnosable
当 Manus 通过本地或 MCP 工具下载图片失败时，系统 SHALL 返回可读错误，至少包含目标保存路径、downloadDir，以及失败原因摘要（例如 HTTP 状态/超时/非图片响应）。

#### Scenario: Non-image response is detected
- **GIVEN** 用户提供的 URL 返回非 `image/*` 的内容类型
- **WHEN** 系统执行下载图片工具
- **THEN** 系统返回可读错误并指出"响应不是图片"
- **AND** 后续 `generatePDF` 嵌图失败时可定位问题原因

#### Scenario: HTTP error is reported with status code
- **GIVEN** 图片 URL 返回 4xx 或 5xx 状态码
- **WHEN** 系统执行下载工具
- **THEN** 系统返回可读错误，包含 HTTP 状态码

### Requirement: Kryo chat-memory files SHALL not be polluted by plaintext logs
系统 SHALL 确保 `.kryo` 会话文件仅包含 Kryo 序列化内容；任何纯文本事件日志 SHALL 写入独立的 `.log` 文件。

#### Scenario: TabooWordAdvisor writes logs without corrupting chat memory
- **GIVEN** 系统启用 `TabooWordAdvisor` 且启用文件型 ChatMemory
- **WHEN** 发生敏感词拒绝并记录事件
- **THEN** 事件日志写入 `<chat-memory-dir>/*.log`
- **AND** 不会对 `<chat-memory-dir>/<conversationId>.kryo` 进行追加写入

### Requirement: PDF generation SHALL tolerate non-BMP characters
系统 SHALL 在 PDF 生成前对内容进行非 BMP 字符（emoji 等）净化，避免 iText 因编码限制抛出 `This encoder only accepts BMP codepoints` 异常。

#### Scenario: Emoji in content does not crash PDF generation
- **GIVEN** `content` 中包含 emoji 字符（如 🌟、1️⃣）
- **WHEN** 系统执行 `generatePDF`
- **THEN** PDF 生成成功，不抛出编码异常
- **AND** 返回信息包含"已移除 N 个非 BMP 字符"的提示（若有移除）

