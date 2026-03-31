## ADDED Requirements
### Requirement: Coach panel SHALL restore persisted cloud images
系统 SHALL 在重新进入 Coach 会话时，从后端加载该会话的已持久化图片元数据，并直接使用云端图片 URL 渲染预览。

#### Scenario: Refresh page and restore coach image previews
- **GIVEN** 用户之前在 Coach 会话中生成过图片资产
- **AND** 这些图片已经持久化到 `conversation_images`
- **WHEN** 用户刷新页面并重新打开该会话
- **THEN** 前端会请求该会话的图片列表
- **AND** Manus 预览面板使用返回的云端 URL 成功渲染图片

#### Scenario: Session without persisted images keeps preview empty
- **GIVEN** 某个 Coach 会话没有任何已持久化图片
- **WHEN** 用户打开该会话
- **THEN** 前端不会显示旧的本地缓存图片
- **AND** 预览区域保持空状态
