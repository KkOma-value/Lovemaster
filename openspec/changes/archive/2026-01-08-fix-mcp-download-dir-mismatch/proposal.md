# Change: Fix MCP download directory mismatch for PDF image embedding

## Why
当前图片“能下载但写不进 PDF”的最常见原因是：主应用与 `mcp-servers` 分别用各自进程的 `System.getProperty("user.dir") + "/tmp"` 作为根目录，导致图片下载落在 `.../mcp-servers/tmp/download/`，而 PDF 生成工具只会从主应用的 `.../<repoRoot>/tmp/download/` 读取。

这会造成：
- MCP 工具返回“下载成功”，但主应用侧 `generatePDF` 实际找不到图片文件。
- 行为依赖“从哪里启动进程”（工作目录），不稳定且难排查。

## What Changes
- 引入可配置的共享文件根目录（例如 `app.file-save-dir`），主应用与 `mcp-servers` 均使用同一配置决定 `download/` 与 `pdf/` 的落盘位置。
- 当未配置时，保持兼容：默认仍使用 `${user.dir}/tmp`。
- 工具返回信息与日志增加“解析后的下载目录/期望路径”，用于一眼判断是否发生目录不一致。

## Impact
- Affected specs: `manus-agent`
- Affected code (apply 阶段才改):
  - `src/main/java/org/example/springai_learn/constant/FileConstant.java`
  - `src/main/java/org/example/springai_learn/tools/PDFGenerationTool.java`
  - `src/main/java/org/example/springai_learn/tools/ResourceDownloadTool.java`
  - `mcp-servers/src/main/java/org/example/mcpservers/server/ImageSearchTool.java`

## Non-Goals
- 不实现“PDF 直接嵌入远程 URL 图片”的能力（仍要求图片先落盘到共享 download 目录）。
- 不扩展复杂 Markdown 渲染（仍维持文本 + `![](...)` 图片的最小支持）。
