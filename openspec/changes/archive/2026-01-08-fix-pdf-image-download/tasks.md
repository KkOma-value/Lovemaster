## 1. Investigation
- [x] 1.1 复现：输入包含 `![](gugong.jpg)` 的 content，确认生成 PDF 中图片缺失。
- [x] 1.2 确认图片存放约定：`System.getProperty("user.dir") + "/tmp/download"` 与当前运行目录一致。
- [x] 1.3 复现 MCP 不可用：确认主应用因排除 `McpClientAutoConfiguration` 导致 MCP tools 未注册。

## 2. Fix
- [x] 2.1 扩展 `PDFGenerationTool.generatePDF`：解析 Markdown 图片语法并按顺序渲染文本与图片。
- [x] 2.2 路径解析规则：当图片路径为纯文件名（如 `gugong.jpg`）时，解析为 `tmp/download/gugong.jpg`；若传入的是相对路径，则同样相对于 `tmp/download/`。
- [x] 2.3 缺图处理：当图片不存在/不可读时，返回可读错误（列出缺失图片清单与期望路径），并避免生成缺图 PDF。
- [x] 2.4 启用 MCP client：移除启动类对 `McpClientAutoConfiguration` 的排除，并配置 `spring.ai.mcp.client.initialized=false` 以避免 server 不可达时启动崩溃。
- [x] 2.5 在 `mcp-servers` 模块新增 `downloadImage(url, fileName)` 工具：下载并保存到 `tmp/download/`。

## 3. Validation
- [x] 3.1 单元测试：PDF 生成在图片存在时成功嵌入（验证 PDF 至少包含 1 个 embedded image XObject）。
- [x] 3.2 单元测试：图片缺失时返回明确错误信息（包含文件名与期望目录）。
- [ ] 3.3 集成验证：启动 `mcp-servers`（SSE 或 STDIO），通过 Manus 调用 `downloadImage` 下载到 `tmp/download/`，随后 `generatePDF` 能嵌入。（需要本地运行 MCP server + LLM 环境手工验证）
- [x] 3.4 `openspec validate fix-pdf-image-download --strict` 通过。
