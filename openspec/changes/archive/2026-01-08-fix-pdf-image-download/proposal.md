# Change: Fix PDF local image embedding + enable MCP image download

## Why
- 当前 `generatePDF` 工具只把传入内容当作纯文本写入 PDF，Markdown 图片语法（例如 `![](gugong.jpg)`）不会被解析，因此图片不会出现在 PDF 中。
- 你已经实现并配置了 MCP 服务器用于图片能力，但主应用在启动类中排除了 `McpClientAutoConfiguration`，导致 MCP 客户端实际未启用，工具不可用。
- 你希望：PDF 中能看到这些图片内容；MCP 提供下载能力，并把图片保存在 `tmp/download/` 下，供 PDF 嵌入使用。

## What Changes
- 更新 `generatePDF` 的渲染策略：支持解析 Markdown 的图片语法 `![](fileName)`，并从 `tmp/download/<fileName>` 加载本地图片嵌入到 PDF 中。
- 当图片文件缺失或无法读取时：返回清晰的可读提示（包含期望路径/文件名），并给出“先下载到 tmp/download 再生成 PDF”的建议；避免静默生成缺图 PDF。
- 启用（或以可配置方式启用）Spring AI MCP 客户端，使 Manus 能调用 MCP 服务器暴露的下载图片工具。
- 在 `mcp-servers` 模块中提供 `downloadImage(url, fileName)` 工具：下载图片并保存到 `tmp/download/<fileName>`，用于后续 PDF 嵌入。

## Impact
- Affected specs: manus-agent
- Affected code (expected):
  - `src/main/java/org/example/springai_learn/tools/PDFGenerationTool.java`
  - `src/main/java/org/example/springai_learn/SpringAiLearnApplication.java`
  - `src/main/resources/application.yml`（如需切换 MCP client 配置方式）
  - `mcp-servers/src/main/java/...`（新增/调整下载图片工具）
  - `src/main/resources/mcp-servers.json`（如需更新 MCP servers 配置/命令）
  - tests

## Out of Scope
- 不做“从 PDF 中直接拉取远程 URL 图片并嵌入”的能力（PDF 仅嵌入本地 `tmp/download/` 中的图片）。
- 不新增前端页面/交互。
- 不改变 LoveApp 聊天链路行为（仅影响 Manus 的工具调用与 PDF 生成）。
