## MODIFIED Requirements
### Requirement: Manus executes autonomously without user confirmation
系统 SHALL 确保 Manus 在每个步骤中至少调用一个工具（除了最终 `doTerminate`），不输出任何询问用户确认的文本，直到任务完成或达到最大步骤数。当任务明确要求图片、照片、视觉参考时，系统 SHALL 在结束前完成可直接渲染的图片资产创建，而不是仅停留在搜索结果 URL。

#### Scenario: Complex task requires multiple tool calls
- **GIVEN** 用户请求需要多步骤完成（如 "生成包含图片的上海旅行PDF"）
- **WHEN** Manus 执行任务
- **THEN** 每个步骤日志输出 `KkomaManus选择了 N 个工具来使用`，其中 N > 0
- **AND** 不输出包含 "请确认" 或 "用户确认" 的文本
- **AND** 最终以 `doTerminate` 工具调用结束，或达到最大步骤数

#### Scenario: Missing information handled autonomously
- **GIVEN** 任务参数不完整（如未指定天数、未提供具体地点等）
- **WHEN** Manus 处理任务
- **THEN** 系统基于合理默认值自主决策（如默认3天、选择热门景点）
- **AND** 直接调用工具执行，不询问用户确认
- **AND** 工具调用日志显示选择了 N 个工具（N > 0）

#### Scenario: Coach image task completes with cloud-renderable artifacts
- **GIVEN** 用户在 Coach 模式中要求“照片”“图片”或“实景图”
- **WHEN** Manus 完成任务并结束工具调用循环
- **THEN** 至少有成功上传到 Supabase Storage 的图片资产被写入会话元数据
- **AND** 系统不会仅凭原始外链搜索结果就终止

### Requirement: Coach image artifacts SHALL persist in Supabase Storage
当 Coach 模式生成或下载图片资料用于前端展示时，系统 SHALL 将图片文件保存到 Supabase Storage，并在 `conversation_images` 中持久化该图片的元数据，供历史会话重新加载。

#### Scenario: Downloaded image is uploaded without local persistence dependency
- **GIVEN** Coach 模式执行图片相关工具，得到有效图片源 URL
- **WHEN** 系统创建该图片资产
- **THEN** 系统直接将图片内容上传到 Supabase Storage
- **AND** `conversation_images` 持久化 `conversation_id`、`file_name`、`source_url`、`storage_path`、`public_url`
- **AND** 前端收到的文件 URL 为可直接渲染的 Supabase 图片链接

#### Scenario: Search preview does not create persisted artifact
- **GIVEN** 系统仅获得 `searchImage` 的预览 URL
- **WHEN** 尚未完成云端上传
- **THEN** 系统 MAY 通过 SSE 展示预览
- **AND** 不会在 `conversation_images` 中写入该预览记录

#### Scenario: Supabase upload failure is diagnosable
- **GIVEN** 外部图片可访问，但 Supabase Storage 未配置或上传失败
- **WHEN** 系统尝试创建会话图片资产
- **THEN** 工具结果包含明确失败原因
- **AND** 不写入无效的 `conversation_images` 记录
