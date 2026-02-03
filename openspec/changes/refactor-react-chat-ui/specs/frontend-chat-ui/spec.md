## ADDED Requirements
### Requirement: React 聊天 UI SHALL 清理 SSE 资源
系统 SHALL 在流式响应完成或错误时，关闭 EventSource 并清理加载/流式状态。

#### Scenario: 流式响应正常完成
- **WHEN** SSE 收到 `[DONE]`
- **THEN** UI SHALL 关闭 EventSource
- **AND** UI SHALL 清理 loading 与 streaming 状态
- **AND** UI SHALL 允许继续发送下一条消息

#### Scenario: 流式响应异常中断
- **WHEN** SSE 发生错误或被异常关闭
- **THEN** UI SHALL 关闭 EventSource
- **AND** UI SHALL 清理 loading 与 streaming 状态
- **AND** UI SHALL 显示可恢复的错误提示

### Requirement: React 聊天 UI SHALL 安全处理未初始化会话
系统 SHALL 在会话 ID 未就绪时阻止发送，或自动创建并选择新会话后再发起 SSE。

#### Scenario: 会话未初始化时发送
- **WHEN** 用户在会话尚未选定时点击发送
- **THEN** UI SHALL 创建/选择会话并再发起 SSE
