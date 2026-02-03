## 1. Investigation
- [x] 1.1 确认 `pom.xml` 中 `com.itextpdf:font-asian` scope，验证当前运行方式（IDE / `spring-boot:run` / 打包 jar）是否包含该依赖。
- [x] 1.2 复现 `generatePDF` 生成中文内容时的异常堆栈，确认失败点仅来自字体创建。

## 2. Fix
- [x] 2.1 将 `com.itextpdf:font-asian` 调整为运行时可用（移除 `test` scope 或改为默认 scope）。
- [x] 2.2 调整 `PDFGenerationTool` 的字体创建逻辑，使其在字体不可用时返回明确错误信息（并保留日志）。

## 3. Validation
- [x] 3.1 增加回归测试：校验 `pom.xml` 中 `font-asian` 不允许为 `test` scope。
- [x] 3.2 增加/更新 PDF 生成测试：中文内容生成不抛出字体识别异常，且 PDF 可提取到文本。
- [x] 3.3 `openspec validate fix-pdf-font-runtime --strict` 通过。
