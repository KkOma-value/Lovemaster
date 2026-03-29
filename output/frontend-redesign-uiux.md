# Lovemaster Frontend Redesign UI/UX 规范

## 1. 设计语言 (Design Language)
- **关键词**：温暖 (Warm), 感性 (Sensual), 通透 (Lucent), 优雅 (Elegant)。
- **视觉风格**：Glassmorphism (毛玻璃) 结合现代极简。去除所有硬性边框(Solid Borders)，使用阴影 (Soft Shadows) 和半透明度 (Opacity) 构建层级关系。

## 2. 核心元素规范

### 2.1 颜色 (Color Palette)
- **主白 (Surface)**: `#FFFFFF` 或 `rgba(255, 255, 255, 0.8)` 用于卡片和输入框。
- **主背景 (Background)**: 选用温馨的人物水彩或抽象形状模糊图。上覆一层 `rgba(245, 240, 235, 0.3)` 使得文字清晰。
- **强调色 (Accent)**: `#CD9B86` 或 `#C4A49A` (灰粉红/肉色)，用于爱心图标、发送按钮悬浮等。
- **文字主色 (Typography Heavy)**: `#2C2623`，优雅而不刺眼。
- **文字副色 (Typography Light)**: `#7A6F6A`。

### 2.2 字体 (Typography)
- **标语 / 大标题**：采用优雅的衬线字体组合，体现柔和的质心。如：“我是你的恋爱助手，今天有什么可以帮忙的？”。
  - `font-size: 28px - 32px`
  - `letter-spacing: 0.05em`
- **正文 / 输入**：采用清晰的无衬线体。
  - `font-size: 15px - 16px`

### 2.3 形状与空间 (Shapes & Spacing)
- **圆角 (Border Radius)**:
  - 侧边栏整体：`16px` 或 `24px`。
  - 卡片和大输入板：`24px`。
  - 胶囊输入框/按钮：`999px` (完全的半圆弧)。
- **内边距 (Padding)**: 强调大量留白。
  - 胶囊输入框：`12px 24px`。
  - 卡片：`32px 40px`。

### 2.4 交互细节 (Micro-Interactions)
- **Hover 态**：所有可点击元素（如历史记录项、发送按钮）在 Hover 时需有亮度的略微提升 `filter: brightness(1.05)`，以及轻微的上浮 `transform: translateY(-1px)`，配合 `box-shadow` 的柔和扩散。
- **Transition**：`all 0.3s cubic-bezier(0.4, 0, 0.2, 1)`。
- **Focus 态**：输入框获得焦点时，外围毛玻璃容器的透明度应略微降低（更白），同时阴影加深以突出焦点区域，不能有突兀的黑色 `outline`。

## 3. 典型页面构建参照
- **图一 (主初始状态)**：
  - 中间有一个圆润的胶囊型输入框，附带右侧的淡红色发送按钮图标。
  - 界面通透，高度居中。
- **图二 (分析场景状态)**：
  - 中部标题文字变为分析引导。
  - 下方出现一个包含“附件”和“发送”的复合圆角巨型面板，用于更复杂的输入和拖拽上传。
  - 侧边栏的“新对话”展示为一个按钮，底部有 User Profile 模块。
