## 1. Investigation
- [x] 1.1 复现并记录 Manus 工具调用失败的典型日志/异常（至少覆盖：工具未找到、执行异常、会话历史类型不匹配）
- [x] 1.2 对比 LoveApp 的 `.tools(...)` 成功路径与 Manus 自定义 ToolCallingManager 路径，确认差异点

## 2. Implementation
- [x] 2.1 为 `ToolCallingManager` 显式配置工具解析/执行所需的 resolver（基于 `availableTools`）
- [x] 2.2 明确 DashScope 工具调用选项：避免自动代理与手动执行冲突（统一为一种机制）
- [x] 2.3 修复 `act()` 对 `conversationHistory()` 的结构假设：不要强制将最后一条消息 cast 为 `ToolResponseMessage`
- [x] 2.4 当工具不存在/执行失败时，返回可读错误并允许下一步继续（除 `terminate` 触发 FINISHED）

## 3. Optional Config & Degradation
- [x] 3.1 `search-api.api-key` 为空时，WebSearch 工具要么不注册，要么返回明确"未配置"的错误（不影响其它工具）
- [x] 3.2 补充 README 或配置模板说明 Manus 工具调用所需的最小配置项

## 4. Validation
- [x] 4.1 增加最小单元测试：模拟一个固定工具调用响应，验证工具可执行且会话历史不会抛异常
- [x] 4.2 运行 `mvn -DskipTests=false test`（如项目当前无测试，则至少本次新增测试通过）
