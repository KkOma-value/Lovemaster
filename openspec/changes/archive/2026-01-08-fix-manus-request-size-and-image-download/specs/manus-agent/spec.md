# manus-agent Delta Specification: fix-manus-request-size-and-image-download

## ADDED Requirements

### Requirement: Manus model requests SHALL stay within provider input limits
系统 SHALL 在调用 DashScope 进行 chat completion 前，对待发送的 `systemPrompt + messages` 应用请求大小预算，避免触发 provider 侧的 `Range of input length should be [1, 1000000]` 错误。

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
系统 SHALL 对可能产生大输出的工具施加输出预算；当输出超过阈值时，系统 SHALL 将全文外部化保存，并仅将“片段 + 引用指针”写回会话历史。

#### Scenario: ReadFile output is externalized when too large
- **GIVEN** Manus 调用 `ReadFile(fileName)`
- **AND** 文件内容长度超过输出阈值
- **WHEN** 系统写回工具执行结果到会话历史
- **THEN** 会话历史仅包含内容片段与保存路径指针
- **AND** 全量内容可在 `<file-save-dir>/tool-output/` 下找到

### Requirement: Image download failures SHALL be diagnosable
当 Manus 通过本地或 MCP 工具下载图片失败时，系统 SHALL 返回可读错误，至少包含目标保存路径、downloadDir，以及失败原因摘要（例如 HTTP 状态/超时/非图片响应）。

#### Scenario: Non-image response is detected
- **GIVEN** 用户提供的 URL 返回非 `image/*` 的内容类型
- **WHEN** 系统执行下载图片工具
- **THEN** 系统返回可读错误并指出“响应不是图片”
- **AND** 后续 `generatePDF` 嵌图失败时可定位问题原因

### Requirement: Kryo chat-memory files SHALL not be polluted by plaintext logs
系统 SHALL 确保 `.kryo` 会话文件仅包含 Kryo 序列化内容；任何纯文本事件日志 SHALL 写入独立的 `.log` 文件。

#### Scenario: TabooWordAdvisor writes logs without corrupting chat memory
- **GIVEN** 系统启用 `TabooWordAdvisor` 且启用文件型 ChatMemory
- **WHEN** 发生敏感词拒绝并记录事件
- **THEN** 事件日志写入 `<chat-memory-dir>/*.log`
- **AND** 不会对 `<chat-memory-dir>/<conversationId>.kryo` 进行追加写入
