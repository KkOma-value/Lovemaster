# Tasks: auth-and-image-upload

## 实现顺序：前端优先 → 后端 → 联调

---

## Phase 1: 后端基础设施（必须先于前端联调）

### Task 1.1: Maven 依赖 + 配置
- [ ] `pom.xml` 添加 spring-boot-starter-security, spring-boot-starter-data-jpa, jjwt (api/impl/jackson)
- [ ] `application.yml` 添加 JPA 配置 (hibernate ddl-auto: update, PostgreSQL dialect)
- [ ] `application.yml` 添加 JWT 配置 (secret, access/refresh expiration)
- [ ] `application.yml` 添加 multipart 配置 (max-file-size: 5MB, max-request-size: 10MB)
- [ ] `application.yml` 添加图片上传目录配置
- [ ] 验证：`mvn clean compile` 通过

### Task 1.2: 数据库实体 + Repository
- [ ] `User.java` JPA 实体 (id, name, email, passwordHash, avatarUrl, createdAt, updatedAt)
- [ ] `RefreshToken.java` JPA 实体 (id, userId, token, expiresAt, createdAt)
- [ ] `UserImage.java` JPA 实体 (id, userId, fileName, originalName, fileSize, contentType, imageType, createdAt)
- [ ] `UserRepository.java` (findByEmail, existsByEmail)
- [ ] `RefreshTokenRepository.java` (findByToken, deleteByUserId)
- [ ] `UserImageRepository.java` (findByUserId, findByUserIdAndImageType)
- [ ] 验证：启动应用，确认表自动创建

### Task 1.3: JWT 服务
- [ ] `JwtService.java` — generateAccessToken, generateRefreshToken, validateToken, extractUserId, extractExpiration
- [ ] 使用 HMAC-SHA256 签名
- [ ] Access Token payload: { sub: userId, email, name, iat, exp }
- [ ] Refresh Token payload: { sub: userId, type: "refresh", iat, exp }

### Task 1.4: Spring Security 配置
- [ ] `SecurityConfig.java` — SecurityFilterChain bean
  - 公开: /api/auth/register, /api/auth/login, /api/auth/refresh, /api/health, /api/images/**（GET读取）
  - 受保护: 其余所有 /api/** 端点
  - Session: STATELESS
  - CSRF: 禁用
  - CORS: 引用现有 CorsConfig
- [ ] `JwtAuthenticationFilter.java` — OncePerRequestFilter
  - 从 Authorization header 或 URL 参数 (token) 提取 JWT
  - 验证并设置 SecurityContext
- [ ] `UserDetailsServiceImpl.java` — 从 UserRepository 加载用户
- [ ] 更新 `CorsConfig.java` — 允许 Authorization header, 暴露必要响应头

### Task 1.5: 认证 Controller + Service
- [ ] `AuthService.java`
  - register(name, email, password) → 验证 + BCrypt哈希 + 创建用户 + 生成令牌
  - login(email, password) → 验证凭据 + 生成令牌
  - logout(userId) → 删除 refresh token
  - refreshToken(token) → 验证 + 生成新令牌对
- [ ] `AuthController.java`
  - POST /api/auth/register
  - POST /api/auth/login
  - POST /api/auth/logout
  - POST /api/auth/refresh
  - GET /api/auth/me
- [ ] DTO: RegisterRequest, LoginRequest, AuthResponse
- [ ] 验证：Postman/curl 测试注册 → 登录 → 获取用户 → 登出

### Task 1.6: 图片上传 Controller + Service
- [ ] `ImageStorageService.java`
  - store(MultipartFile, userId, imageType) → 验证类型/大小 + UUID重命名 + 存储 + 记录数据库
  - getImagePath(userId, type, fileName) → 返回文件路径
  - 存储路径: {save-dir}/images/{userId}/{type}/{uuid}.{ext}
- [ ] `ImageController.java`
  - POST /api/images/upload (认证) — 接收 MultipartFile + type
  - GET /api/images/{userId}/{type}/{fileName} (公开) — 读取图片
- [ ] DTO: ImageUploadResponse
- [ ] 验证：curl 上传图片 + 访问图片 URL

### Task 1.7: 数据隔离改造
- [ ] `AiController.java` — SSE 端点注入当前 userId (从 SecurityContext 或 URL token 参数)
- [ ] `ChatSessionController.java` — 所有接口增加 userId 过滤
- [ ] `LoveApp.java` — 聊天记忆路径改为 `{save-dir}/chat-memory/{userId}/{chatId}`
- [ ] 聊天记忆文件目录按 userId 隔离
- [ ] 验证：两个不同用户登录后聊天记录互不可见

---

## Phase 2: 前端认证

### Task 2.1: 安装依赖 + 基础设施
- [ ] `npm install browser-image-compression`
- [ ] 创建 `src/contexts/AuthContext.jsx` — Provider + useAuth hook
  - state: { user, accessToken, isAuthenticated, isLoading }
  - 方法: login, register, logout, refreshToken
  - 初始化: 尝试用 refresh token 恢复登录状态
- [ ] 创建 `src/services/authApi.js` — 认证 API 调用封装

### Task 2.2: API 拦截器
- [ ] 更新 `src/services/chatApi.js` — Axios 实例添加请求/响应拦截器
  - 请求: 自动附加 Authorization header
  - 响应: 401 → 尝试刷新 → 重试 → 失败跳转登录
- [ ] SSE 连接: URL 追加 `?token={accessToken}` 参数

### Task 2.3: 登录页 + 注册页
- [ ] `src/pages/Auth/LoginPage.jsx` — 登录表单 (邮箱+密码)
- [ ] `src/pages/Auth/RegisterPage.jsx` — 注册表单 (昵称+邮箱+密码+确认密码)
- [ ] `src/pages/Auth/AuthPage.module.css` — 玻璃态表单样式 (参考 UIUX 文档)
- [ ] 表单验证: 邮箱格式、密码长度、密码匹配
- [ ] 错误提示样式
- [ ] 密码强度指示器 (注册页)
- [ ] 页面过渡动画 (Framer Motion)
- [ ] 响应式: 移动端/平板适配

### Task 2.4: 路由守卫 + 路由改造
- [ ] `src/components/Auth/ProtectedRoute.jsx` — 未登录重定向到 /login
- [ ] 更新 `App.jsx` — 添加 AuthProvider, 路由分组 (公开/受保护)
- [ ] 登录/注册成功后跳转到 /chat
- [ ] 已登录用户访问 /login 自动跳转到 /

### Task 2.5: 侧边栏用户信息
- [ ] 更新 `ChatSidebar.jsx` — 底部添加用户信息栏
  - 显示头像 (默认头像 fallback)、昵称、邮箱
  - 登出按钮
  - 头像可点击更换

---

## Phase 3: 前端图片上传

### Task 3.1: 图片上传 Hook + Service
- [ ] `src/services/imageApi.js` — 图片上传 API 调用
- [ ] `src/hooks/useImageUpload.js`
  - compressImage(file) — 调用 browser-image-compression
  - uploadImage(compressedFile, type) — POST FormData
  - state: { isCompressing, isUploading, progress, preview, error }

### Task 3.2: 聊天图片上传组件
- [ ] `src/components/Chat/ImageUpload.jsx`
  - 附件按钮 (输入框左侧)
  - 图片选择 (input[type=file] accept="image/*")
  - 压缩进度条
  - 缩略图预览 + 文件大小变化
  - 移除按钮
- [ ] 更新 `ChatInput.jsx` — 集成 ImageUpload 组件
- [ ] 更新 `ChatMessages.jsx` — 支持渲染图片消息
- [ ] 图片灯箱 (点击放大查看)

### Task 3.3: 头像上传
- [ ] 侧边栏头像点击触发文件选择
- [ ] 压缩 (maxSizeMB: 0.5, maxWidthOrHeight: 400)
- [ ] 上传成功后更新 AuthContext 中的 user.avatar

---

## Phase 4: 联调 + 质量

### Task 4.1: 端到端联调
- [ ] 注册新用户 → 自动登录 → 聊天页
- [ ] 登出 → 重新登录 → 历史记录恢复
- [ ] 用户 A/B 数据隔离验证
- [ ] SSE 聊天 + Token 认证正常
- [ ] 图片上传 → 聊天中显示 → 点击放大
- [ ] 头像上传 → 侧边栏更新
- [ ] Token 过期 → 自动刷新 → 无感续期

### Task 4.2: 前端质量
- [ ] `npm run lint` 零 error
- [ ] 响应式: 375/768/1024/1440px 断点正常
- [ ] 玻璃态效果与现有页面一致

### Task 4.3: 后端质量
- [ ] `mvn clean compile` 通过
- [ ] API 错误码正确 (400/401/409/413)
- [ ] 路径遍历防护验证
- [ ] Content-Type 白名单验证

---

## 检查点

| 检查点 | 触发条件 | 审查内容 |
|--------|---------|---------|
| CP1 | Task 1.5 完成 | 认证 API 可用, curl 测试通过 |
| CP2 | Task 1.7 完成 | 数据隔离生效, 两用户聊天互不可见 |
| CP3 | Task 2.4 完成 | 前端认证流程完整, 路由守卫生效 |
| CP4 | Task 3.2 完成 | 图片上传+压缩+预览+发送完整 |
| CP5 | Task 4.1 完成 | 全链路联调通过 |
