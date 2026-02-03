## MODIFIED Requirements

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

## ADDED Requirements

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
