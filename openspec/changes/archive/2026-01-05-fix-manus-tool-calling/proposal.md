# Change: Fix Manus tool/function calling reliability

## Why
当前 Manus（`/ai/manus/chat`）模式下的工具调用/函数调用存在失败或不稳定行为：工具执行器可能找不到已注册工具、执行结果的会话历史结构不符合代码假设，导致运行时异常或无法继续推理-行动循环。

## What Changes
- 修正 Manus 代理的工具调用执行链路，使工具调用可稳定执行并将结果写回会话上下文
- 明确并统一 DashScope 工具调用相关选项（避免“框架自动代理工具调用”和“手动执行工具调用”互相冲突）
- 让“可选外部能力”（如 WebSearch 的 `search-api.api-key`、邮件账号等）缺失时降级，不影响其它工具调用
- 补充后端日志与最小化回归测试，能快速定位“工具未找到/参数不匹配/执行异常”

## Impact
- Affected code (expected):
  - `src/main/java/org/example/springai_learn/agent/ToolCallAgent.java`
  - `src/main/java/org/example/springai_learn/agent/KkomaManus.java`
  - `src/main/java/org/example/springai_learn/tools/ToolRegistration.java`
  - (可能) 相关 tools 类的参数校验与错误返回
- Affected specs:
  - **ADDED** `manus-agent`（新能力，描述 Manus 模式的工具调用行为与降级策略）

## Out of Scope
- 不新增前端交互/页面
- 不改变 LoveApp 现有行为（除非复用同一工具调用机制且保持兼容）
