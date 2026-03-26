# Research: 用户认证 + 图片上传功能

## 1. better-auth 设计模式参考

### 1.1 概述
better-auth 是一个 TypeScript 认证框架（MIT 协议，YC 背书），虽不直接适用于 Java/Spring，但其**数据库 Schema 设计模式和认证流程**值得参考。

### 1.2 核心数据库表设计（参考）
| 表名 | 用途 | 关键字段 |
|------|------|---------|
| users | 用户主表 | id, email, password_hash, name, avatar, email_verified, created_at |
| sessions | 会话管理 | id, user_id, token, expires_at, ip_address, user_agent |
| accounts | OAuth 账号关联 | id, user_id, provider, provider_account_id |
| verifications | 验证令牌 | id, identifier, token, expires_at, type |

### 1.3 关键设计决策
- **会话存储在数据库**（非纯 JWT），支持即时撤销
- **HTTP-only Cookie** 传递会话令牌，防 XSS
- **BCrypt 密码哈希**，自带盐值
- **多会话支持**，用户可在多设备同时登录
- **CSRF 保护**通过 SameSite Cookie 属性

### 1.4 认证流程
```
注册: 验证输入 → 检查重复 → 哈希密码 → 创建用户 → 创建会话 → 返回令牌
登录: 验证凭据 → 比对密码 → 创建会话 → 返回令牌
登出: 删除会话记录 → 清除 Cookie
```

---

## 2. browser-image-compression 技术分析

### 2.1 概述
browser-image-compression 是一个轻量级客户端图片压缩库，周下载量 ~732k，支持在浏览器端压缩 JPEG/PNG/WebP/BMP 图片后再上传服务器。

### 2.2 核心 API
```javascript
import imageCompression from 'browser-image-compression';

const options = {
  maxSizeMB: 1,              // 最大文件大小 (MB)
  maxWidthOrHeight: 1920,    // 最大宽/高
  useWebWorker: true,        // Web Worker 多线程压缩
  onProgress: (p) => {},     // 进度回调 (0-100)
  preserveExif: false,       // 保留 EXIF
  initialQuality: 0.8        // 初始质量 (0-1)
};

const compressedFile = await imageCompression(imageFile, options);
```

### 2.3 集成模式
```
用户选择图片 → 前端 browser-image-compression 压缩
  → FormData 封装压缩后 Blob
  → POST /api/upload 上传到后端
  → 后端 MultipartFile 接收 → 存储到本地/S3
```

### 2.4 关键特性
- **Web Worker 支持**：不阻塞主线程，移动端友好
- **压缩率高**：典型照片可压缩 85%+
- **零依赖**：纯浏览器 API 实现
- **进度回调**：支持 UI 进度条展示

---

## 3. Spring Boot 3.x + Spring Security 6.x 认证方案

### 3.1 JWT vs Session 对比

| 维度 | JWT | Session |
|------|-----|---------|
| 状态 | 无状态，易横向扩展 | 有状态，需集中存储 |
| 撤销 | 需黑名单机制 | 删除即撤销 |
| XSS 防护 | 存 localStorage 有风险 | HttpOnly Cookie 安全 |
| 适用场景 | 微服务/API | 传统 Web 应用 |

### 3.2 推荐方案：JWT + HttpOnly Cookie 混合
- **Access Token**: 短生命周期（30min），存 React 内存状态
- **Refresh Token**: 长生命周期（7天），存 HttpOnly Cookie
- **优点**: 兼顾安全性（HttpOnly 防 XSS）和无状态（JWT 易扩展）

### 3.3 Spring Security 6.x 配置模式
- 使用 `SecurityFilterChain` Bean（非已废弃的 WebSecurityConfigurerAdapter）
- `SessionCreationPolicy.STATELESS` 禁用服务端 Session
- 自定义 `JwtAuthenticationFilter` 拦截请求
- `BCryptPasswordEncoder` 密码哈希
- 自定义 `AuthenticationEntryPoint` 返回 JSON 错误

### 3.4 SSE + Spring Security 的关键挑战
- SSE 的后续事件使用 ASYNC Dispatcher，可能绕过认证过滤器
- **解决方案**：将 SecurityContext 存储在 HttpServletRequest 属性中，确保异步重分发时认证信息持续可用

### 3.5 Per-User 数据隔离
- 所有用户数据表添加 `user_id` 外键
- Service 层通过 `SecurityContextHolder.getContext().getAuthentication()` 获取当前用户
- 查询时强制过滤 `WHERE user_id = :currentUserId`
- 文件存储按 `user_id` 子目录隔离

---

## 4. 文件/图片上传 Spring Boot 实现

### 4.1 MultipartFile 接收
- `spring.servlet.multipart.max-file-size` 控制单文件大小
- `spring.servlet.multipart.max-request-size` 控制请求总大小
- 安全：UUID 重命名、路径遍历防护、Content-Type 验证

### 4.2 存储策略选择
| 策略 | 适用场景 | 复杂度 |
|------|---------|--------|
| 本地文件系统 | 学习项目/小规模 | 低 |
| S3/OSS | 生产环境 | 中 |
| 数据库 BLOB | 不推荐 | 高 |

**推荐**：本地文件系统（项目已有 `app.file-save-dir` 配置），按 `user_id` 子目录组织。

### 4.3 完整上传流程
```
前端: 选图 → browser-image-compression 压缩 → FormData POST
后端: MultipartFile 接收 → 验证类型/大小 → UUID 重命名 → 存储到 {save-dir}/images/{userId}/ → 返回访问 URL
```

---

## 5. 当前项目现状分析

### 5.1 现有基础
- PostgreSQL + PgVector 已配置（可复用数据库连接）
- 无 Spring Security 依赖、无用户表、无认证配置
- CORS 已配置（允许所有来源）
- 文件下载已有（FileController），但无上传
- 前端无认证状态管理
- React 19 + Vite + Tailwind + Framer Motion

### 5.2 需要新增
| 层 | 新增项 |
|----|--------|
| Maven | spring-boot-starter-security, jjwt, spring-boot-starter-data-jpa |
| 数据库 | users, user_images 表 |
| 后端 | SecurityConfig, AuthController, UserEntity, ImageUploadController |
| 前端 | AuthContext, LoginPage, RegisterPage, useAuth hook, ImageUpload 组件 |

---

## 6. 同类产品参考

### 6.1 AI 聊天类产品认证模式
| 产品 | 认证方式 | 数据隔离 | 图片上传 |
|------|---------|---------|---------|
| ChatGPT | OAuth + Email | user_id 关联所有对话 | 支持（GPT-4V） |
| Claude.ai | OAuth + Email | workspace/conversation 级别 | 支持上传 |
| 通义千问 | 阿里云账号 | account_id 关联 | 支持图片理解 |
| Kimi | 手机号 + Email | user_id 关联 | 支持文件上传 |

### 6.2 共性功能
- 注册/登录页面简洁（单页表单，支持 OAuth）
- 登录后自动跳转聊天页
- 侧边栏显示当前用户历史对话
- 用户头像/昵称显示在侧边栏底部
- 图片上传集成在聊天输入框中（附件按钮）

### 6.3 差异化方向
- 恋爱主题视觉：保持现有暖陶土 + 玻璃态风格
- 图片上传用于聊天上下文（而非仅头像）
- 简化认证流程：仅 Email/密码注册登录（MVP 阶段不需 OAuth）
