## ADDED Requirements

### Requirement: Manus file-generation tool writes provided content
系统 SHALL 确保 Manus 调用的文件/文档生成类工具会将模型提供的内容写入目标文件，并在内容缺失时返回可读错误而不是生成空白文件。

#### Scenario: PDF tool writes provided text
- **GIVEN** Manus 模型返回 `generatePDF` 工具调用，参数包含非空 `fileName` 与 `content`
- **WHEN** 系统执行工具
- **THEN** 生成的 PDF 文件包含传入的 `content` 文本（可在生成文件中可见）
- **AND** 工具执行结果反馈包含生成路径

#### Scenario: PDF content missing
- **GIVEN** Manus 模型返回 `generatePDF` 工具调用但 `content` 为空或仅包含空白
- **WHEN** 系统执行工具
- **THEN** 系统返回明确的参数错误提示并不生成空白 PDF 文件
- **AND** 会话继续可重新发起带有效内容的调用
