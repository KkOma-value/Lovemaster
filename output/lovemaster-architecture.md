# Architecture: Lovemaster 前端重构 — Dribbble 插画背景

## 1. 改动范围（仅前端，零后端）

```
springai-front-react/
├── src/assets/illustrations/
│   ├── bg-home.png                    ★ 新增：Dribbble 参考图1（首页背景）
│   ├── bg-chat.png                    ★ 新增：Dribbble 参考图2（聊天页背景）
│   └── bg-lovers.png                  ○ 删除：旧背景图
├── src/styles/global.css              ★ Token 增强 + body 背景逻辑移除
├── src/pages/Home/HomePage.jsx        ★ 添加背景图 + 玻璃态增强
├── src/pages/Home/HomePage.module.css ★ 背景图 + 玻璃态样式
├── src/pages/Chat/ChatPage.jsx        ★ 添加背景图容器
├── src/pages/Chat/ChatPage.module.css ★ 背景图 + 布局微调
├── src/components/Chat/ChatArea.module.css        ★ 玻璃态气泡/header
├── src/components/Chat/ChatInput.jsx              ○ 微调样式
├── src/components/Sidebar/ChatSidebar.module.css  ★ 玻璃态侧边栏
├── src/components/ManusPanel/ManusPanel.module.css ★ 玻璃态面板
└── src/components/ui/                 ○ Button/Input Token 继承（小改）
```

**不改动：**
- 所有 hooks / services
- 组件逻辑（JSX 仅改样式 className 和背景图引用）
- 后端全部代码

---

## 2. 背景图管理策略

### 文件放置
```
src/assets/illustrations/
├── bg-home.png      # ref_image_1: Sunflower Love 合集 (~500KB 压缩)
├── bg-chat.png      # ref_image_2: Couple on Bench 情侣 (~500KB 压缩)
├── ai-avatar.svg    # AI 头像 (保留)
├── hero-lovers.svg  # Hero 插画 (保留)
└── leaf-deco.svg    # 叶片装饰 (保留)
```

### 背景图应用方式
**方案: 页面级 CSS 背景**

- `HomePage` 容器通过 `.page` 类设置 `background-image: url(bg-home.png)`
- `ChatPage` 容器通过 `.chatPageRoot` 类设置 `background-image: url(bg-chat.png)`
- 移除 `global.css` 中 `body` 上的背景图（改由各页面自控）
- 使用 `background-size: cover; background-attachment: fixed` 保证全覆盖

---

## 3. CSS Token 增强

### 新增 Token（青绿色系）
```css
:root {
  /* ── 新增：Teal 植物装饰色（来自参考图叶片） ── */
  --color-teal:          #3A8B7F;   /* 主青绿 */
  --color-teal-light:    #4FA89A;   /* 浅青绿 */
  --color-teal-muted:    rgba(58, 139, 127, 0.15);  /* 青绿浅底 */
  
  /* ── 调整：背景更贴近参考图 ── */
  --bg-base:             #F5C9A8;   /* 暖桃沙色（参考图主底色） */
  
  /* ── 新增：玻璃态专用 Token ── */
  --glass-bg:            rgba(255, 253, 249, 0.72);
  --glass-bg-strong:     rgba(255, 253, 249, 0.85);
  --glass-border:        rgba(255, 255, 255, 0.3);
  --glass-blur:          20px;
}
```

### 保留 Token（完全兼容）
所有现有的 `--color-primary`, `--text-*`, `--shadow-*`, `--radius-*` 保持不变。

---

## 4. 玻璃态 (Glassmorphism) 标准

### 标准 Glass Mixin
```css
/* 标准玻璃态 */
.glass {
  background: var(--glass-bg);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  border: 1px solid var(--glass-border);
}

/* 强玻璃态（文字密集区域） */
.glass-strong {
  background: var(--glass-bg-strong);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid var(--glass-border);
}
```

### 各组件透明度层级
| 组件 | 背景不透明度 | 模糊值 | 说明 |
|------|-------------|--------|------|
| Header | 0.88 | 16px | 需清晰可读 |
| Sidebar | 0.80 | 20px | 让插画若隐若现 |
| 功能卡片 | 0.75 | 20px | Hero 区突出玻璃效果 |
| 消息气泡 (AI) | 0.90 | 12px | 文字可读性优先 |
| 消息气泡 (用户) | 不变 | 无 | 陶土渐变保持不变 |
| ManusPanel | 0.85 | 20px | 代码/终端可读性 |
| 输入框 | 0.90 | 12px | 输入清晰 |

---

## 5. 组件改动清单（按优先级）

### P0 — 资源 + 全局基础
1. 复制 Dribbble 图片到 `src/assets/illustrations/`（bg-home.png, bg-chat.png）
2. `global.css` — 添加青绿色 Token + 玻璃态 Token + 移除 body 背景图

### P1 — 首页（视觉冲击最大）
3. `HomePage.module.css` — `.page` 加背景图 + 卡片玻璃态增强
4. `HomePage.jsx` — 无逻辑变动

### P2 — 聊天核心
5. `ChatPage.module.css` — 根容器加背景图
6. `ChatArea.module.css` — header/气泡/输入框 玻璃态
7. `ChatSidebar.module.css` — 玻璃态侧边栏

### P3 — 面板
8. `ManusPanel.module.css` — 玻璃态面板

---

## 6. 实现注意事项

1. **图片优化**: ref_image 原图约 1600×1200px，需确保文件大小合理（< 800KB）。如过大可通过 Vite 构建自动优化
2. **Tailwind 兼容**: 项目使用 Tailwind + CSS Modules 混合；新 Token 在 `global.css` 中，无需改 `tailwind.config.js`
3. **`background-attachment: fixed`**: 在移动 Safari 上可能不生效，需回退为 `scroll`
4. **渐进方式**: 先设置背景图 → 全局生效 → 再调整各组件透明度
5. **旧背景**: `bg-lovers.png` 可保留或删除，移除 body 级引用即可
