# Design: update-frontend-love-theme

## Intent
让“恋爱主题”成为前端用户可见内容的默认心智：用户打开首页就明确这是恋爱/情感对话产品，而不是通用或技术演示项目。

## Approach
- 以“文案与入口收敛”为主，不引入新交互与新页面。
- 尽量复用现有 Element Plus 组件与页面布局，仅调整展示内容。

## Options
### Option A（Love-only）
- 首页仅保留 LoveApp 入口；Manus 不对用户呈现。
- 优点：主题最纯粹；改动最少；最符合你的诉求。
- 注意：若保留 Manus 路由用于开发调试，需避免首页露出。

### Option B（Keep Manus but rebrand）
- 保留 Manus，但定位为“恋爱工具/恋爱教练模式”，示例 prompts 与文案全部恋爱化。
- 优点：功能更丰富；不减少入口。
- 风险：后端 agent 的默认行为可能偏工具/技术，需要更多提示词约束（本变更不涉及后端提示词调整）。

## Content Rules (non-functional)
- 首页不得出现与恋爱无关的示例问题（如 Java、Spring、Docker、前端最佳实践等）。
- 默认文案避免强调技术栈（如 Spring Boot/Vue3），除非放在 README 或隐藏区域。
