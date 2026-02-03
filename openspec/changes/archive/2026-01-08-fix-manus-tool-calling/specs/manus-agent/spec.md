# manus-agent Specification Delta

## ADDED Requirements

### Requirement: Manus executes autonomously without user confirmation
系统 SHALL 确保 Manus 在每个步骤中至少调用一个工具（除了最终 `doTerminate`），不输出任何询问用户确认的文本，直到任务完成或达到最大步骤数。

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
