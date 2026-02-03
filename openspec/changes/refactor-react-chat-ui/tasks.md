## 1. Implementation
- [x] 1.1 复核 React 聊天流中 SSE 生命周期的状态路径与错误路径
- [x] 1.2 抽取 SSE 连接通用逻辑，移除重复实现并统一完成/错误处理
- [x] 1.3 精简 `ChatPage` 中发送与删除逻辑，避免陈旧状态与重复设置
- [x] 1.4 清理明显的冗余日志与未使用参数/状态

## 2. Validation
- [ ] 2.1 在 springai-front-react 目录执行 `npm run lint`
- [ ] 2.2 手动验证：LoveApp 与 Coach 模式下发送消息、结束流、断网错误、删除会话与新建会话流程
