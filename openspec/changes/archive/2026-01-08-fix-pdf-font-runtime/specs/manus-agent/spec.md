## ADDED Requirements

### Requirement: PDF generation supports runtime CJK fonts
系统 SHALL 在运行时具备生成包含中文等 CJK 字符的 PDF 能力，不得因字体依赖仅存在于测试类路径而导致生成失败或空白 PDF。

#### Scenario: Runtime PDF generation does not fail due to missing font-asian
- **GIVEN** 系统以非测试方式运行（生产运行时类路径）
- **WHEN** Manus 调用 `generatePDF` 生成包含中文字符的内容
- **THEN** PDF 生成不会抛出“字体不可识别/缺失”类异常
- **AND** 生成的 PDF 不为空白且可读

#### Scenario: Regression guard for dependency scope
- **GIVEN** 项目构建执行单元测试
- **WHEN** `pom.xml` 中将 `com.itextpdf:font-asian` 误设为 `test` scope
- **THEN** 测试应失败并提示该依赖必须在运行时可用
