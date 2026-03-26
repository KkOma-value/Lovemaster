# UIUX: 用户认证系统 + 图片上传功能

> 基准风格：延续现有暖陶土 + 玻璃态 (Glassmorphism) 设计语言
> 新增页面：登录页、注册页
> 新增组件：图片上传、用户信息栏

---

## 1. 视觉方向

### 核心原则
认证页面和图片上传组件必须与现有暖陶土 + 玻璃态风格保持一致，不引入新的设计语言。

| 维度 | 现有风格 | 认证/上传组件 |
|------|---------|-------------|
| 背景 | Dribbble 插画全屏 | 复用首页背景 (bg-home.png) |
| UI 层 | 玻璃态卡片 | 登录/注册表单使用玻璃态 |
| 色调 | 暖桃 + 陶土 + 青绿 | 完全继承，不引入新色 |
| 圆角 | 24-32px | 表单卡片 28px，输入框 12px |
| 字体 | Playfair Display + Inter | 完全继承 |

---

## 2. 登录页设计

### 2.1 布局
```
┌─────────────────────────────────────────────────┐
│              bg-home.png (全屏背景)              │
│                                                  │
│         ┌────────────────────────────┐           │
│         │    🌻 Lovemaster           │           │
│         │    (Logo + 品牌名)          │           │
│         │                            │           │
│         │    邮箱                     │           │
│         │    ┌──────────────────┐    │           │
│         │    │ your@email.com   │    │           │
│         │    └──────────────────┘    │           │
│         │                            │           │
│         │    密码                     │           │
│         │    ┌──────────────────┐    │           │
│         │    │ ••••••••         │    │           │
│         │    └──────────────────┘    │           │
│         │                            │           │
│         │    [    登  录    ]         │           │
│         │                            │           │
│         │    没有账号？注册           │           │
│         └────────────────────────────┘           │
│                                                  │
└─────────────────────────────────────────────────┘
```

### 2.2 玻璃态表单卡片
```css
.authCard {
  background: rgba(255, 253, 249, 0.78);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 28px;
  padding: 48px 40px;
  max-width: 420px;
  width: 90%;
  box-shadow: 0 8px 32px rgba(196, 123, 90, 0.12);
}
```

### 2.3 输入框样式
```css
.authInput {
  background: rgba(255, 250, 245, 0.85);
  border: 1.5px solid rgba(196, 123, 90, 0.2);
  border-radius: 12px;
  padding: 14px 16px;
  font-family: 'Inter', sans-serif;
  font-size: 15px;
  color: var(--text-primary);        /* #2D1B0E */
  transition: border-color 0.2s, box-shadow 0.2s;
}

.authInput:focus {
  border-color: var(--color-primary); /* #C47B5A */
  box-shadow: 0 0 0 3px rgba(196, 123, 90, 0.15);
  outline: none;
}

.authInput::placeholder {
  color: var(--text-muted);          /* #B09080 */
}
```

### 2.4 登录按钮
```css
.authButton {
  background: linear-gradient(135deg, #C47B5A 0%, #DCA080 100%);
  color: #FFFAF5;
  border: none;
  border-radius: 14px;
  padding: 14px 0;
  width: 100%;
  font-family: 'Inter', sans-serif;
  font-weight: 600;
  font-size: 16px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.authButton:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(196, 123, 90, 0.35);
}

.authButton:active {
  transform: translateY(0);
}

.authButton:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}
```

### 2.5 错误提示
```css
.errorMessage {
  background: rgba(220, 38, 38, 0.08);
  border: 1px solid rgba(220, 38, 38, 0.2);
  border-radius: 10px;
  padding: 10px 14px;
  color: #B91C1C;
  font-size: 14px;
  margin-bottom: 16px;
}
```

### 2.6 链接样式
```css
.authLink {
  color: var(--color-primary);       /* #C47B5A */
  text-decoration: none;
  font-weight: 500;
}

.authLink:hover {
  color: var(--color-primary-dark);  /* #A0624A */
  text-decoration: underline;
}
```

---

## 3. 注册页设计

### 3.1 布局
与登录页共享同一背景和卡片样式，表单字段增加：
- 昵称（必填）
- 邮箱（必填）
- 密码（必填，≥8位）
- 确认密码（必填）

### 3.2 密码强度指示器
```
弱:   ████░░░░░░  红色 (#DC2626)
中:   ██████░░░░  橙色 (#D97706)
强:   ████████░░  青绿 (#3A8B7F)
很强: ██████████  深青绿 (#2A6B5F)
```

```css
.strengthBar {
  height: 4px;
  border-radius: 2px;
  background: rgba(196, 123, 90, 0.1);
  margin-top: 6px;
  overflow: hidden;
}

.strengthFill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.3s, background 0.3s;
}
```

### 3.3 底部链接
"已有账号？登录" — 样式同登录页链接

---

## 4. 侧边栏用户信息栏

### 4.1 位置
侧边栏最底部，固定不滚动。

### 4.2 布局
```
┌──────────────────────────────┐
│  ┌────┐  用户昵称             │
│  │头像│  user@email.com       │
│  └────┘               [登出]  │
└──────────────────────────────┘
```

### 4.3 样式
```css
.userInfo {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-top: 1px solid rgba(196, 123, 90, 0.1);
  margin-top: auto;
}

.userAvatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid rgba(196, 123, 90, 0.2);
  cursor: pointer;           /* 点击可更换头像 */
}

.userAvatar:hover {
  border-color: var(--color-primary);
}

.userName {
  font-weight: 600;
  font-size: 14px;
  color: var(--text-primary);
}

.userEmail {
  font-size: 12px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
}

.logoutButton {
  margin-left: auto;
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 6px;
  border-radius: 6px;
}

.logoutButton:hover {
  background: rgba(196, 123, 90, 0.1);
  color: var(--text-primary);
}
```

---

## 5. 图片上传组件

### 5.1 聊天输入框中的附件按钮

在现有 ChatInput 的发送按钮左侧添加附件按钮：

```
┌─────────────────────────────────────────────┐
│  [📎]  输入你的消息...              [发送]   │
└─────────────────────────────────────────────┘
```

```css
.attachButton {
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  transition: color 0.2s, background 0.2s;
}

.attachButton:hover {
  color: var(--color-primary);
  background: rgba(196, 123, 90, 0.08);
}
```

### 5.2 图片预览区

选择图片后，在输入框上方显示预览：

```
┌─────────────────────────────────────────────┐
│  ┌──────┐                                    │
│  │ 预览  │  filename.jpg (压缩中... 67%)      │
│  │ 缩略  │  1.2MB → 0.3MB  ✓ 已压缩    [✕]  │
│  └──────┘                                    │
├─────────────────────────────────────────────┤
│  [📎]  输入你的消息...              [发送]   │
└─────────────────────────────────────────────┘
```

```css
.imagePreview {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  background: rgba(255, 253, 249, 0.88);
  border: 1px solid rgba(196, 123, 90, 0.12);
  border-radius: 14px 14px 0 0;
  border-bottom: none;
}

.previewThumbnail {
  width: 56px;
  height: 56px;
  border-radius: 10px;
  object-fit: cover;
  border: 1px solid rgba(196, 123, 90, 0.15);
}

.previewInfo {
  flex: 1;
}

.previewFileName {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.previewSize {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.previewSize .compressed {
  color: var(--color-teal);         /* #3A8B7F 青绿表示成功 */
}

.removeButton {
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  font-size: 18px;
}

.removeButton:hover {
  background: rgba(220, 38, 38, 0.08);
  color: #DC2626;
}
```

### 5.3 压缩进度条

```css
.progressBar {
  height: 3px;
  background: rgba(196, 123, 90, 0.1);
  border-radius: 2px;
  margin-top: 6px;
  overflow: hidden;
}

.progressFill {
  height: 100%;
  background: linear-gradient(90deg, var(--color-primary), var(--color-teal));
  border-radius: 2px;
  transition: width 0.2s ease;
}
```

### 5.4 聊天消息中的图片显示

```css
.messageImage {
  max-width: 280px;
  max-height: 280px;
  border-radius: 16px;
  object-fit: cover;
  cursor: pointer;           /* 点击放大 */
  border: 1px solid rgba(196, 123, 90, 0.12);
  transition: transform 0.2s;
}

.messageImage:hover {
  transform: scale(1.02);
}

/* 图片灯箱 (点击放大查看) */
.imageLightbox {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.75);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  cursor: zoom-out;
}

.lightboxImage {
  max-width: 90vw;
  max-height: 90vh;
  object-fit: contain;
  border-radius: 12px;
}
```

---

## 6. 头像上传交互

### 6.1 触发方式
点击侧边栏底部头像图标 → 弹出文件选择器

### 6.2 头像裁剪预览（简化版）
- 不引入第三方裁剪库（MVP 阶段）
- 前端通过 `maxWidthOrHeight: 400` 和 `maxSizeMB: 0.5` 压缩
- 自动居中裁剪为正方形（CSS `object-fit: cover` + `border-radius: 50%`）

### 6.3 上传状态
```
默认头像 → 选择图片 → 压缩中(旋转动画) → 上传中 → 更新显示
```

---

## 7. 页面过渡动画

### 7.1 登录/注册页进入
```jsx
<motion.div
  initial={{ opacity: 0, y: 20 }}
  animate={{ opacity: 1, y: 0 }}
  transition={{ duration: 0.4, ease: "easeOut" }}
>
  {/* 认证卡片 */}
</motion.div>
```

### 7.2 登录成功过渡
```jsx
// 登录成功后
<motion.div
  exit={{ opacity: 0, scale: 0.95 }}
  transition={{ duration: 0.3 }}
/>
// 过渡到聊天页
```

---

## 8. 响应式适配

### 8.1 登录/注册页
```css
/* 移动端 */
@media (max-width: 480px) {
  .authCard {
    padding: 32px 24px;
    border-radius: 20px;
    margin: 16px;
    width: calc(100% - 32px);
  }
}

/* 平板 */
@media (min-width: 481px) and (max-width: 768px) {
  .authCard {
    max-width: 380px;
  }
}
```

### 8.2 图片预览区
```css
@media (max-width: 480px) {
  .previewThumbnail {
    width: 44px;
    height: 44px;
  }

  .messageImage {
    max-width: 200px;
    max-height: 200px;
  }
}
```

---

## 9. 无障碍 (Accessibility)

### 9.1 表单
- 所有输入框有 `<label>` 关联
- 错误信息使用 `aria-describedby` 关联到输入框
- 登录/注册按钮在加载时使用 `aria-busy="true"`
- Tab 顺序合理：邮箱 → 密码 → 登录按钮 → 注册链接

### 9.2 图片上传
- 附件按钮有 `aria-label="上传图片"`
- 预览区有 `alt` 文本
- 删除按钮有 `aria-label="移除图片"`
- 进度条使用 `role="progressbar"` + `aria-valuenow`

### 9.3 对比度
- 所有文字在玻璃态背景上对比度 ≥ 4.5:1
- 错误信息红色 (#B91C1C) 在浅背景上对比度 ≥ 5:1

---

## 10. 交付检查清单

### 认证页面
- [ ] 登录页在插画背景上正确渲染
- [ ] 注册页表单验证正常
- [ ] 玻璃态卡片效果正确
- [ ] 输入框 focus 状态有陶土色描边
- [ ] 按钮 hover/active/disabled 状态正确
- [ ] 错误信息样式正确
- [ ] 移动端响应式正常

### 用户信息栏
- [ ] 侧边栏底部显示头像、昵称、邮箱
- [ ] 登出按钮功能正常
- [ ] 头像可点击更换

### 图片上传
- [ ] 附件按钮在输入框内正确显示
- [ ] 图片选择后显示预览
- [ ] 压缩进度条正常显示
- [ ] 文件大小变化正确显示
- [ ] 删除按钮可移除预览
- [ ] 聊天中图片消息正确显示
- [ ] 图片点击可放大查看

### 风格一致性
- [ ] 新组件与现有暖陶土风格一致
- [ ] 玻璃态效果参数与现有组件统一
- [ ] 无引入新的设计语言或配色
- [ ] 字体系统一致
