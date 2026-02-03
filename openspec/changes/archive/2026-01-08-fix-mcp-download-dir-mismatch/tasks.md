## 1. Investigation
- [x] 1.1 复现/确认：当主应用与 `mcp-servers` 以不同工作目录启动时，默认 `${user.dir}/tmp` 会导致 `tmp/download` 分叉（通过代码与路径推导确认）。
- [x] 1.2 确认：主应用 `generatePDF` 只会从 `<file-save-dir>/download` 读取图片，并在缺图时回显 expected path（见工具返回）。

## 2. Implementation (apply 阶段)
- [x] 2.1 引入配置项（例如 `app.file-save-dir`）：作为共享文件根目录，默认 `${user.dir}/tmp`。
- [x] 2.2 主应用：`PDFGenerationTool`、`ResourceDownloadTool` 使用共享根目录计算 `download/` 与 `pdf/`。
- [x] 2.3 MCP server：`ImageSearchTool.downloadImage` 使用共享根目录写入 `download/`。
- [x] 2.4 诊断增强：工具返回信息中包含“解析后的 downloadDir / pdfDir”，便于排查。

## 3. Validation
- [x] 3.1 单测：现有 `PDFGenerationToolTest` 继续通过。
- [x] 3.2 集成验证（自动化）：新增主应用侧单测覆盖“配置 file-save-dir 后 PDF 从配置 downloadDir 读图并写入配置 pdfDir”；新增 mcp-servers 侧单测覆盖“配置 file-save-dir 后 downloadImage 写入配置 downloadDir”。
- [x] 3.3 `openspec validate fix-mcp-download-dir-mismatch --strict` 通过。
