# Change: Fix PDF font availability at runtime

## Why
- Manus 调用 `generatePDF` 生成的 PDF 为空白并报错：`Type of font STSongStd-Light is not recognized.`
- 根因初步定位为：`com.itextpdf:font-asian` 在 `pom.xml` 中被设置为 `test` scope，导致运行时（非测试类路径）缺少亚洲字体支持，从而字体创建失败。

## What Changes
- 修正 `font-asian` 依赖的 scope，使其在运行时可用，避免生产环境生成 PDF 时字体不可识别。
- 增强 PDF 生成的字体选择与错误提示：当目标字体不可用时，返回可读错误并建议配置/依赖校验（最小改动，不引入新字体文件）。
- 增加回归验证：用自动化方式防止 `font-asian` 再次被误设为 `test` scope，确保运行时具备字体能力。

## Impact
- Affected specs: manus-agent
- Affected code: pom.xml, tools/PDFGenerationTool.java, tests
