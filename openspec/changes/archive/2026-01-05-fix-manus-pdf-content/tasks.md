## 1. Investigation
- [x] 1.1 复现 Manus 触发 `generatePDF` 的调用，记录请求参数、日志与生成文件内容。
- [x] 1.2 梳理 Manus 工具调用链（模型输出 → ToolCallAgent → PDFGenerationTool），确认参数映射与文件路径处理是否丢失内容。

## 2. Fix
- [x] 2.1 为 `generatePDF` 增加空内容/非法文件名的校验，返回可读错误并避免生成空白文件。
- [x] 2.2 调整 PDF 写入逻辑（编码、字体、流关闭）确保内容正确持久化，并输出生成路径与文件大小日志便于排查。
- [x] 2.3 若发现模型未传递内容，更新 Manus 提示词/工具描述或调用管理逻辑，确保 content 参数可靠传递。

## 3. Validation
- [x] 3.1 添加自动化测试：Manus 工具调用生成的 PDF 含有提供的文本内容。
- [x] 3.2 添加自动化测试：空内容时返回错误且不写入空白 PDF。
- [x] 3.3 运行 openspec validate --strict 及项目测试，确认回归通过。
