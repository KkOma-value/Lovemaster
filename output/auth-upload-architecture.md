# Architecture: 用户认证系统 + 图片上传功能

## 1. 架构概览

### 1.1 系统分层

```
┌─────────────────────────────────────────────────────┐
│                React Frontend                        │
│  AuthContext → LoginPage / RegisterPage              │
│  ImageUpload (browser-image-compression)             │
│  Protected Routes → ChatPage / HomePage              │
├─────────────────────────────────────────────────────┤
│                Spring Boot Backend                   │
│  ┌──────────┐ ┌──────────┐ ┌───────────────────┐   │
│  │AuthFilter│→│Controller│→│    Service Layer   │   │
│  │(JWT)     │ │  Layer   │ │(Auth/Chat/Upload)  │   │
│  └──────────┘ └──────────┘ └───────────────────┘   │
│                       ↓                              │
│  ┌──────────────────────────────────────────────┐   │
│  │           Repository Layer (JPA)              │   │
│  └──────────────────────────────────────────────┘   │
│                       ↓                              │
│  ┌──────────────┐  ┌────────────────────────┐       │
│  │  PostgreSQL   │  │  Local File Storage    │       │
│  │  (users,      │  │  (images/{userId}/...) │       │
│  │   sessions)   │  │                        │       │
│  └──────────────┘  └────────────────────────┘       │
└─────────────────────────────────────────────────────┘
```

### 1.2 技术选型决策

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 认证方式 | JWT (Access + Refresh Token) | 无状态，适合现有 SSE 架构 |
| 密码哈希 | BCrypt (Spring Security 内置) | 业界标准，自带盐值 |
| ORM | Spring Data JPA + Hibernate | 标准化实体管理，现有项目可增量引入 |
| 图片压缩 | browser-image-compression (前端) | 减少服务端负担，不重复造轮子 |
| 图片存储 | 本地文件系统 | 学习项目，已有 file-save-dir 配置 |
| Token 存储 | Access→内存, Refresh→HttpOnly Cookie | 安全与可用性平衡 |

---

## 2. 数据库设计

### 2.1 新增表

#### users 表（参考 better-auth 设计）
```sql
CREATE TABLE users (
    id              VARCHAR(36) PRIMARY KEY,        -- UUID
    name            VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    avatar_url      VARCHAR(500),
    email_verified  BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
```

#### refresh_tokens 表
```sql
CREATE TABLE refresh_tokens (
    id          VARCHAR(36) PRIMARY KEY,
    user_id     VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(500) NOT NULL UNIQUE,
    expires_at  TIMESTAMP NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
```

#### user_images 表
```sql
CREATE TABLE user_images (
    id          VARCHAR(36) PRIMARY KEY,
    user_id     VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    file_name   VARCHAR(255) NOT NULL,
    original_name VARCHAR(255),
    file_size   BIGINT,
    content_type VARCHAR(100),
    image_type  VARCHAR(20) NOT NULL,     -- 'avatar' | 'chat'
    width       INT,
    height      INT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_images_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_user_images_user_id ON user_images(user_id);
```

### 2.2 现有数据迁移

聊天记忆当前存储在文件系统（`/tmp/chat-memory/`, `/tmp/chat-coach/`），改造方案：
- **不迁移数据库**：聊天记忆继续使用文件系统，但文件路径改为 `{save-dir}/chat-memory/{userId}/`
- **会话管理**：`ChatSessionController` 的内存/文件会话增加 user_id 维度

---

## 3. 后端架构

### 3.1 新增 Maven 依赖

```xml
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- Spring Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- JWT (jjwt) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

### 3.2 包结构新增

```
src/main/java/org/example/springai_learn/
├── auth/
│   ├── entity/
│   │   ├── User.java                 # JPA 实体
│   │   ├── RefreshToken.java         # JPA 实体
│   │   └── UserImage.java            # JPA 实体
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── RefreshTokenRepository.java
│   │   └── UserImageRepository.java
│   ├── service/
│   │   ├── AuthService.java          # 注册/登录/登出/刷新
│   │   ├── JwtService.java           # JWT 生成/验证
│   │   └── ImageStorageService.java  # 图片存储管理
│   ├── dto/
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   ├── AuthResponse.java
│   │   └── ImageUploadResponse.java
│   ├── controller/
│   │   ├── AuthController.java       # /api/auth/*
│   │   └── ImageController.java      # /api/images/*
│   └── security/
│       ├── SecurityConfig.java       # Spring Security 配置
│       ├── JwtAuthenticationFilter.java  # JWT 过滤器
│       └── UserDetailsServiceImpl.java   # UserDetailsService 实现
├── config/
│   └── CorsConfig.java              # 更新 CORS 配置
└── controller/
    ├── AiController.java             # 增加 user_id 关联
    └── ChatSessionController.java    # 增加 user_id 过滤
```

### 3.3 Spring Security 配置要点

```
SecurityFilterChain 配置:
├── 公开端点: /api/auth/register, /api/auth/login, /api/auth/refresh, /api/health
├── 受保护端点: /api/ai/**, /api/images/upload, /api/auth/me, /api/auth/logout
├── 图片静态访问: /api/images/{userId}/{fileName} (公开读取或认证读取)
├── Session 策略: STATELESS
├── CSRF: 禁用 (REST API + JWT)
├── CORS: 允许前端域名
└── Filter: JwtAuthenticationFilter 在 UsernamePasswordAuthenticationFilter 之前
```

### 3.4 JWT 流程

```
                    ┌─────────────────────┐
                    │   JwtService        │
                    │                     │
  登录/注册 ──────→ │ generateAccessToken  │──→ 短期 Token (30min)
                    │ generateRefreshToken │──→ 长期 Token (7天)
                    │ validateToken        │──→ 验证签名+过期
                    │ extractUserId        │──→ 从 Token 取 userId
                    └─────────────────────┘
                              ↓
                    ┌─────────────────────┐
                    │ JwtAuthenticationFilter │
                    │                         │
  每个请求 ──────→  │ 1. 从 Header 提取 Token  │
                    │ 2. 验证 Token 有效性     │
                    │ 3. 加载 UserDetails     │
                    │ 4. 设置 SecurityContext  │
                    └─────────────────────────┘
```

### 3.5 SSE 端点安全处理

现有 SSE 端点 (`/api/ai/love_app/chat/sse`, `/api/ai/manus/chat`) 需特殊处理：
- SSE 连接建立时从 JWT 提取 user_id
- 将 user_id 传入 LoveApp/KkomaManus 的聊天方法
- 聊天记忆文件路径改为 `{save-dir}/chat-memory/{userId}/{chatId}`
- SecurityContext 在 ASYNC dispatcher 中通过 request attribute 持久化

### 3.6 图片存储策略

```
{app.file-save-dir}/
├── images/
│   ├── {userId}/
│   │   ├── avatar/
│   │   │   └── {uuid}.jpg
│   │   └── chat/
│   │       ├── {uuid}.png
│   │       └── {uuid}.jpg
```

- 文件名使用 UUID，防止冲突和路径攻击
- Content-Type 白名单：image/jpeg, image/png, image/webp
- 单文件限制 5MB（前端已压缩）
- 访问 URL 格式：`/api/images/{userId}/{type}/{fileName}`

---

## 4. 前端架构

### 4.1 新增依赖

```json
{
  "browser-image-compression": "^2.0.2"
}
```

### 4.2 新增文件结构

```
springai-front-react/src/
├── contexts/
│   └── AuthContext.jsx           # 认证状态管理 (Provider + Hook)
├── pages/
│   ├── Auth/
│   │   ├── LoginPage.jsx         # 登录页面
│   │   ├── RegisterPage.jsx      # 注册页面
│   │   └── AuthPage.module.css   # 共享认证页样式
├── components/
│   ├── Auth/
│   │   └── ProtectedRoute.jsx    # 路由守卫
│   └── Chat/
│       └── ImageUpload.jsx       # 图片上传组件 (压缩+预览+上传)
├── hooks/
│   ├── useAuth.js                # 认证 Hook (登录/注册/登出)
│   └── useImageUpload.js         # 图片上传 Hook (压缩+上传)
└── services/
    ├── authApi.js                # 认证 API 调用
    └── imageApi.js               # 图片上传 API 调用
```

### 4.3 路由改造

```jsx
// App.jsx 路由结构
<Routes>
  {/* 公开路由 */}
  <Route path="/login" element={<LoginPage />} />
  <Route path="/register" element={<RegisterPage />} />

  {/* 受保护路由 */}
  <Route element={<ProtectedRoute />}>
    <Route path="/" element={<HomePage />} />
    <Route path="/chat" element={<ChatPage />} />
    <Route path="/chat/:type" element={<ChatPage />} />
  </Route>
</Routes>
```

### 4.4 AuthContext 设计

```
AuthContext
├── state: { user, accessToken, isAuthenticated, isLoading }
├── login(email, password) → 存 accessToken 到内存, refreshToken 到 Cookie
├── register(name, email, password) → 同上
├── logout() → 清除令牌, 跳转 /login
├── refreshToken() → 用 refreshToken 获取新 accessToken
└── 初始化: 检查 refreshToken Cookie → 自动刷新 accessToken
```

### 4.5 API 请求拦截

```
chatApi.js / authApi.js (Axios 拦截器)
├── 请求拦截: 自动添加 Authorization: Bearer {accessToken}
├── 响应拦截: 401 → 尝试刷新 Token → 重试原请求 → 失败则跳转登录
└── SSE 连接: URL 参数传递 token（SSE 不支持自定义 Header）
```

### 4.6 图片上传组件流程

```
用户点击附件按钮
  → input[type=file] accept="image/*"
  → 选择文件
  → browser-image-compression 压缩 (显示进度条)
  → 压缩完成，显示缩略图预览
  → 用户点发送
  → FormData POST /api/images/upload
  → 返回 imageUrl
  → 消息中包含 imageUrl 发送给 AI
```

---

## 5. 改动影响分析

### 5.1 需修改的现有文件

| 文件 | 改动 | 影响 |
|------|------|------|
| `pom.xml` | 添加 security/jpa/jjwt 依赖 | 编译时 |
| `application.yml` | 添加 JPA/JWT/Upload 配置 | 启动时 |
| `CorsConfig.java` | 收紧 CORS 策略 | 跨域请求 |
| `AiController.java` | 注入当前 user_id 到聊天方法 | 聊天功能 |
| `ChatSessionController.java` | 按 user_id 过滤会话 | 会话管理 |
| `LoveApp.java` | 聊天记忆路径增加 userId 维度 | 数据隔离 |
| `App.jsx` (前端) | 添加 AuthProvider + ProtectedRoute | 路由 |
| `ChatInput.jsx` (前端) | 添加图片上传按钮 | 聊天输入 |
| `chatApi.js` (前端) | 添加 JWT 拦截器 | API 调用 |
| `useChatSessions.js` (前端) | API 调用自动携带 Token | 会话加载 |
| `useSSEConnection.js` (前端) | SSE URL 添加 Token 参数 | SSE 连接 |
| `ChatSidebar.jsx` (前端) | 底部添加用户信息和登出按钮 | 侧边栏 |

### 5.2 不改动

- AI Agent 核心逻辑（BaseAgent, ReActAgent, ToolCallAgent, KkomaManus）
- Tool 层（所有工具实现不变）
- RAG 层（向量存储不变）
- MCP Server 模块
- 前端样式系统（保持现有玻璃态/插画风格）

---

## 6. 配置新增

```yaml
# application.yml 新增
spring:
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 10MB

# JWT 配置
app:
  jwt:
    secret: ${JWT_SECRET:your-256-bit-secret-key-for-jwt-signing}
    access-token-expiration: 1800000    # 30 分钟
    refresh-token-expiration: 604800000 # 7 天

  # 图片上传配置
  image:
    upload-dir: ${APP_FILE_SAVE_DIR:${user.dir}/tmp}/images
    max-size: 5242880  # 5MB
    allowed-types: image/jpeg,image/png,image/webp
```
