# Design: PDF image embedding + MCP image download

## Goals
- 让 `generatePDF` 能把 Markdown 内容中的本地图片（`![](gugong.jpg)`）嵌入到 PDF。
- 图片文件统一来源：`<projectRoot>/tmp/download/`（即 `FileConstant.FILE_SAVE_DIR + "/download"`）。
- 让 Manus 能调用 MCP 服务器提供的图片下载工具 `downloadImage(url, fileName)`，把图片放入上述目录。

## Non-Goals
- 不实现“PDF 内联下载远程图片 URL”的能力。
- 不实现复杂 Markdown（表格/代码块/嵌套列表等）到 PDF 的完整渲染引擎；只覆盖文本段落 + 图片（满足当前行程示例）。

## Current State
- `PDFGenerationTool` 使用 iText 直接写入 `new Paragraph(content)`，不会解析 Markdown，因此 `![](...)` 不会产生图片。
- 主应用启动类排除 `McpClientAutoConfiguration`，即使 `application.yml` 配置了 `spring.ai.mcp.client.sse.connections`，MCP client 也不会被创建，MCP tools 不会出现在 Manus 工具集合中。
- `mcp-servers` 模块当前提供 `searchImage(query)`（返回 URL 列表），但没有“下载并保存到本地文件”的工具。

## Proposed Changes
### 1) Minimal Markdown -> PDF rendering
- 在 `generatePDF` 中实现一个最小解析器：按行/段落扫描文本，识别 `![](path)` 语法。
- 渲染策略：
  - 普通文本：写入 Paragraph（保留换行/段落间距）。
  - 图片：把 `path` 解析到 `tmp/download/<path>`，读取本地文件并用 iText `Image` 添加到 document。
- 错误策略（更利于用户定位问题）：
  - 任意图片缺失/读取失败：汇总缺失列表并返回错误；不生成缺图 PDF。

### 2) MCP client enablement
- 目标是让 MCP tools 可被 Manus 调用，但不破坏本地启动体验。
- 方案候选：
  1. **直接启用 AutoConfiguration**：移除 `McpClientAutoConfiguration` 的 exclude，让 Spring AI MCP client 按 `application.yml` 生效。
     - 风险：MCP server 不可达时是否影响启动需要验证。
  2. **条件启用**（推荐）：通过 profile 或 property 控制是否启用 MCP client（默认关闭，避免本地未启 MCP server 时影响启动）。
     - 实现方式将在 apply 阶段根据 Spring AI MCP client 的实际开关能力确定（必要时自建最小配置类并用 `@ConditionalOnProperty` 包裹）。

### 3) MCP downloadImage tool
- 在 `mcp-servers` 增加 `downloadImage(url, fileName)`：
  - 下载 URL 内容并保存到 `<user.dir>/tmp/download/<fileName>`。
  - 返回保存路径（供 Manus 后续生成 PDF 时引用）。
- 该工具不依赖第三方 API key，避免触发“仓库不应提交 secrets”的约束。

## Compatibility
- 不改变现有 `generatePDF(fileName, content)` 的签名；只是扩展对 `content` 的解释（支持 Markdown 图片）。
- 若 content 不包含图片语法，行为保持与现状一致。

## Observability
- PDF 生成时在日志中输出：解析到的图片数量、解析后的本地路径、嵌入成功/失败原因。
- MCP 下载工具日志：URL、目标文件名、保存路径、下载异常。
