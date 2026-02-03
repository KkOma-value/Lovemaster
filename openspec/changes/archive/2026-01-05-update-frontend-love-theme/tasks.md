# Tasks: update-frontend-love-theme

## 1. Implementation
- [x] 选择改版方案（Option A 或 Option B），锁定页面范围与需要保留的入口。
- [x] 更新首页文案与结构：
  - [x] 标题/副标题改为恋爱主题
  - [x] 移除或替换“技术特色”区域为恋爱相关亮点（不新增复杂组件）
  - [x] 处理“Manus”入口（按所选 Option 隐藏/改名/移除）
  - [x] 移除或实现“项目源码”按钮（如保留则使用你提供的链接）
  - [x] 页脚文案从技术栈描述改为恋爱主题描述
- [x] 更新示例 prompts：
  - [x] LoveApp 示例问题替换为恋爱场景
  - [x] （Option B）Manus 示例问题替换为恋爱场景
- [x] （可选）路由收敛：
  - [x] （Option A）首页不再展示 Manus 入口；如决定移除路由，更新 `router/index.js`
- [x] 更新前端 README 的产品描述，使其与恋爱主题一致（保持接口说明不变）。

## 2. Validation
- [x] `springAI-front`：`npm run build` 通过
- [x] 首页与聊天页可正常访问
- [x] 示例问题与页面文案均为恋爱主题
