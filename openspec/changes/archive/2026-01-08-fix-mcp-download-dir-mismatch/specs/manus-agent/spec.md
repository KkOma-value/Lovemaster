## ADDED Requirements

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
