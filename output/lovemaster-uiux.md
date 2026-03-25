# UIUX: Lovemaster 视觉设计规范 — Dribbble 插画背景版

> 基准参考：两张 Dribbble 浪漫插画  
> 风格定义：**插画背景 + 玻璃态 UI (Illustration Background + Glassmorphism)**

---

## 1. 视觉方向

### 核心意象
「像透过一块磨砂玻璃，看到一幅温暖的恋爱故事画」

| 维度 | 当前状态 | 目标状态 |
|------|---------|---------|
| 背景 | 纯色/渐变 #FAF7F2 | Dribbble 插画全屏渲染 |
| UI 层 | 实色卡片 | 玻璃态 (Glassmorphism) |
| 色调 | 陶土/奶油为主 | 暖桃 + 青绿植物双色调 |
| 质感 | 扁平实色 | 毛玻璃透出手绘插画 |

---

## 2. 色彩系统

### 主调色板

```
暖桃背景（参考图主调）────────────────────────
#F5C9A8  全局背景主色（暖桃沙）
#FDE8D8  更浅的奶油桃色
#FAF7F2  玻璃态内层底色（带透明度使用）

陶土主色（保留）──────────────────────────────
#C47B5A  主色调 (Terracotta) — 按钮/用户气泡
#DCA080  浅陶土 (Light Terra) — 渐变辅助
#A0624A  深陶土 (Deep Terra) — hover/强调

★ 新增：青绿植物色 ──────────────────────────
#3A8B7F  主青绿 (Teal) — 装饰/标签/强调
#4FA89A  浅青绿 — hover/辅助
rgba(58,139,127,0.15)  青绿浅底 — 标签背景

金沙辅色（保留）──────────────────────────────
#C9A87C  金沙 (Sand Gold)
#E8C4A0  浅桃金 (Peach Gold)

文字色（保留）────────────────────────────────
#2D1B0E  深暖棕 — 主文字 (WCAG AAA)
#7A5C47  中暖棕 — 次要文字 (WCAG AA)
#B09080  淡棕 — 占位符
#FFFAF5  暖白 — 深色背景上文字
```

### 玻璃态 Token
```css
--glass-bg:          rgba(255, 253, 249, 0.72);   /* 标准 */
--glass-bg-strong:   rgba(255, 253, 249, 0.85);   /* 需要可读性 */
--glass-bg-light:    rgba(255, 253, 249, 0.55);   /* 装饰性 */
--glass-border:      rgba(255, 255, 255, 0.3);
--glass-blur:        20px;
```

---

## 3. 背景图应用规范

### 首页（ref_image_1: Sunflower Love 合集）
```css
.page {
  background-image: url('bg-home.png');
  background-size: 105% 105%; /* Scale up slightly to crop encoded black borders */
  background-position: center;
  background-attachment: fixed;
  background-repeat: no-repeat;
  background-color: #F5C9A8;   /* 图片加载前的回退色 */
}
```

### 聊天页（ref_image_2: Couple on Bench）
```css
.chatPageRoot {
  background-image: url('bg-chat.png');
  background-size: 105% 105%; /* Scale up slightly to crop encoded black borders */
  background-position: center;
  background-attachment: fixed;
  background-repeat: no-repeat;
  background-color: #F5C9A8;
}
```

---

## 4. 组件视觉规范

### 4.1 首页 Hero 卡片
```
背景: rgba(255, 253, 249, 0.72) + blur(20px)  ← 玻璃态
边框: 1px solid rgba(255, 255, 255, 0.3)
圆角: 32px
内部: 保留渐变 overlay 作为色彩层
主插画: hero-lovers.svg (SVG 内联, 右侧漂浮)
文字: 白色 + text-shadow 确保可读
hover: translateY(-6px) + shadow 增强
```

### 4.2 功能卡片
```
背景: rgba(255, 253, 249, 0.75) + blur(20px)
边框: 1px solid rgba(255, 255, 255, 0.3)
圆角: 28px
Icon 区: 可使用青绿色渐变或陶土色渐变
hover: 透明度提升至 0.88 + shadow 增强
```

### 4.3 侧边栏 (ChatGPT 风格重构)
```
背景: rgba(245, 239, 230, 0.80) + blur(20px)
顶部: 醒目的“新对话”按钮 (无边框，主要渐变色)
列表项: 移除冗余边框，悬浮呈现整行浅色背景
对齐: 更紧凑的 padding，参考 ChatGPT 左侧的极简文件树风格
```

### 4.4 聊天 Header
```
背景: rgba(250, 247, 242, 0.88) + blur(16px)
底部边框: 1px solid var(--glass-border)
标题: Playfair Display 500, 18px
```

### 4.5 消息气泡

**AI 气泡**
```
背景: rgba(255, 253, 249, 0.90) + blur(12px)
边框: 1px solid rgba(196,123,90,0.18)
左侧装饰: 3px solid --color-primary
圆角: 0 20px 20px 20px
文字: #2D1B0E (深棕，高对比)
```

**用户气泡**
```
背景: linear-gradient(135deg, #C47B5A 0%, #DCA080 100%)  ← 不变
圆角: 20px 0 20px 20px
文字: #FFFAF5
```

### 4.6 输入框
```
结构: 移除右侧“回首页”按钮，只保留输入框与发送按钮
背景: rgba(255, 250, 245, 0.90) + blur(12px)
边框: 1.5px solid var(--glass-border)
圆角: 24px
focus: border-color --color-primary + warm shadow
```

### 4.7 ManusPanel
```
背景: rgba(250, 247, 242, 0.85) + blur(20px)
Tab 指示器: --color-primary 下划线
终端文字: #3D2B1F on glass background
```

---

## 5. 字体系统（保留不变）

同上一版，略。

---

## 6. 动效规范（新增 Framer Motion）

引入 `framer-motion` 处理路由级动画：

```jsx
// 页面过渡包装组件
<motion.div
  initial={{ opacity: 0, y: 10 }}
  animate={{ opacity: 1, y: 0 }}
  exit={{ opacity: 0, scale: 0.98 }}
  transition={{ duration: 0.3 }}
>
  {children}
</motion.div>
```

```css
/* 玻璃态 hover 过渡 */
.glass-card {
  transition: background 0.3s ease,
              backdrop-filter 0.3s ease,
              box-shadow 0.3s cubic-bezier(0.4,0,0.2,1),
              transform 0.3s cubic-bezier(0.4,0,0.2,1);
}
```

---

## 7. 响应式断点规范

同上一版，追加背景图响应式：
```css
/* 移动端取消 fixed attachment（Safari 兼容） */
@media (max-width: 768px) {
  .page, .chatPageRoot {
    background-attachment: scroll;
  }
}
```

---

## 8. 交付检查清单

### 背景图
- [ ] 参考图1 作为首页背景正确渲染
- [ ] 参考图2 作为聊天页背景正确渲染
- [ ] 背景图质量清晰、无拉伸变形
- [ ] 图片加载前显示回退色

### 玻璃态
- [ ] 所有卡片/面板背景可透出插画
- [ ] blur 效果正常（Chrome/Firefox/Safari）
- [ ] 文字在玻璃态背景上清晰可读（对比度 ≥ 4.5:1）

### 功能回归
- [ ] LoveApp 聊天：发消息、流式响应、历史加载正常
- [ ] Coach 聊天：ManusPanel 打开/关闭/任务/终端/预览正常
- [ ] 会话：新建、删除、切换正常
- [ ] npm run lint 零 error
