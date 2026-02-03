# Change: update-frontend-love-theme

## Why
当前前端首页与聊天入口呈现出较强的“通用/技术演示”气质（例如 Manus 技术助手、技术特色、技术类示例问题、项目源码入口等），与“恋爱主题”的产品定位不一致，影响用户对产品的第一印象与使用心智。

## What Changes
- 首页与全局文案改为“恋爱主题优先”（标题、副标题、卡片描述、页脚文案）。
- 移除或弱化与恋爱无关的入口与信息（例如 Manus 技术助手入口、技术特色区、技术栈说明、项目源码按钮）。
- 调整示例问题（example prompts）为恋爱/关系主题（脱单、沟通、相处、复合、情绪等）。
- 保持现有功能不变：
  - LoveApp SSE 聊天仍通过 `/api/ai/love_app/chat/sse`
  - 主题切换与整体布局保持

## Scope Options (choose one during apply)
- **Option A（推荐，最小改动且符合主题）**：前端只对用户展示 LoveApp（恋爱助手）。Manus 相关页面/路由可以保留但不在首页入口呈现（或直接移除路由）。
- **Option B**：保留 Manus 入口，但改名与文案为“恋爱工具/恋爱教练模式”，并将示例问题全部换成恋爱话题（仍调用 `/api/ai/manus/chat`）。

## Impact
- Affected frontend files (expected):
  - `springAI-front/src/views/HomePage.vue`
  - `springAI-front/src/components/LoveAppChat.vue`
  - `springAI-front/src/components/ManusChat.vue`（仅在选择 Option B 时）
  - `springAI-front/src/router/index.js`（仅在选择 Option A 且移除/隐藏路由时）
  - `springAI-front/README.md`（文案同步）

## Risks
- 如果选择 Option A 并移除 Manus 路由，属于“用户可见功能收缩”，需要确认是否仍要保留后端 Manus 能力用于调试。
- 如果选择 Option B，Manus 的 agent 能力可能仍偏“工具/技术”，但可通过提示词与示例问题引导其更恋爱化。

## Open Questions (need your confirmation)
1. 你希望采用 Option A 还是 Option B？
2. 首页标题你更偏好哪种风格？
   - A) “恋爱助手 / 情感陪伴”
   - B) “脱单助手 / 恋爱教练”
   - C) 你给一个固定名字
3. 你是否要保留“项目源码”按钮？（保留则需要你提供跳转链接，否则建议移除）
