# Change: Fix Manus PDF tool output

## Why
- Manus 模式调用 `generatePDF` 工具时生成的 PDF 文件为空白，未写入模型提供的内容，破坏工具调用可用性。

## What Changes
- 校验并修复 `generatePDF` 工具的入参和写入逻辑，避免空内容或编码问题导致生成空白 PDF。
- 调整 Manus 工具调用链（提示词或调用管理）确保模型传入的内容字段被正确接收和持久化，并增加可观测性。
- 为 Manus PDF 工具调用添加回归验证（自动化测试），覆盖成功与异常路径。

## Impact
- Affected specs: manus-agent
- Affected code: tools/PDFGenerationTool.java, agent/ToolCallAgent.java, controller/AiController.java, Manus 工具调用相关测试
