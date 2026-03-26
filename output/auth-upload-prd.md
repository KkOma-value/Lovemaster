# PRD: 用户认证系统 + 图片上传功能

## 1. 背景与目标

### 问题陈述
当前 Lovemaster 项目是一个完全开放的 AI 聊天应用，无任何用户认证和数据隔离：
- 所有用户共享同一个聊天空间
- 聊天记录存储在服务器本地文件系统，无用户归属
- 无法区分不同用户的对话历史
- 不支持图片上传（缺少多模态交互能力）

### 目标
1. **用户认证**：实现注册/登录功能，参考 better-auth 的数据库和会话设计模式
2. **数据隔离**：基于 user_id 隔离每个用户的聊天记录和文件
3. **图片上传**：前端使用 browser-image-compression 压缩，后端接收存储，支持在聊天中发送图片

---

## 2. 用户故事

### US-1: 用户注册
> 作为新用户，我希望通过邮箱和密码注册账号，以便拥有自己的私有聊天空间。

**验收标准**：
- 注册表单包含：昵称、邮箱、密码、确认密码
- 邮箱格式校验 + 唯一性检查
- 密码最少 8 位，包含字母和数字
- 注册成功后自动登录并跳转聊天页
- 注册失败显示清晰错误信息

### US-2: 用户登录
> 作为已注册用户，我希望通过邮箱和密码登录，以便访问我的历史聊天记录。

**验收标准**：
- 登录表单包含：邮箱、密码
- 登录成功后跳转聊天页，侧边栏加载该用户的历史对话
- 登录失败显示"邮箱或密码错误"（不暴露具体哪个错误）
- 支持"记住我"选项（延长 Refresh Token 有效期）

### US-3: 用户登出
> 作为已登录用户，我希望能安全登出，以防他人访问我的聊天记录。

**验收标准**：
- 侧边栏底部显示用户信息和登出按钮
- 登出后清除本地令牌、跳转登录页
- 登出后原有 Token 立即失效

### US-4: 数据隔离
> 作为用户，我希望只看到自己的聊天记录，不会看到其他用户的对话。

**验收标准**：
- 聊天会话列表只显示当前用户创建的会话
- 聊天历史记录只返回当前用户的消息
- 文件访问受限于当前用户的文件目录
- API 请求无有效令牌时返回 401

### US-5: 图片上传（聊天中）
> 作为用户，我希望在聊天输入框中上传图片，让 AI 能理解图片内容。

**验收标准**：
- 聊天输入框旁有"附件"按钮
- 支持选择图片文件（JPEG/PNG/WebP）
- 选择后前端自动压缩（maxSizeMB: 2, maxWidthOrHeight: 1920）
- 显示压缩进度条
- 压缩完成后预览缩略图
- 发送时图片和文本一起提交
- 聊天记录中显示图片消息

### US-6: 用户头像上传
> 作为用户，我希望上传自己的头像，以便在聊天中显示。

**验收标准**：
- 用户信息区域可点击更换头像
- 头像裁剪为正方形（前端处理）
- 压缩至 maxSizeMB: 0.5
- 上传成功后立即更新显示

---

## 3. 功能需求

### 3.1 认证系统

#### 3.1.1 注册 API
- `POST /api/auth/register`
- 请求体：`{ name, email, password }`
- 响应：`{ user: { id, name, email, avatar }, accessToken, refreshToken }`
- 错误码：400（参数错误）、409（邮箱已注册）

#### 3.1.2 登录 API
- `POST /api/auth/login`
- 请求体：`{ email, password }`
- 响应：同注册
- 错误码：401（凭据错误）

#### 3.1.3 登出 API
- `POST /api/auth/logout`
- Header：`Authorization: Bearer {accessToken}`
- 后端使 Refresh Token 失效

#### 3.1.4 令牌刷新 API
- `POST /api/auth/refresh`
- 请求体：`{ refreshToken }` 或从 HttpOnly Cookie 读取
- 响应：`{ accessToken, refreshToken }`

#### 3.1.5 获取当前用户 API
- `GET /api/auth/me`
- Header：`Authorization: Bearer {accessToken}`
- 响应：`{ id, name, email, avatar, createdAt }`

### 3.2 图片上传

#### 3.2.1 通用图片上传 API
- `POST /api/images/upload`
- Content-Type：`multipart/form-data`
- Header：`Authorization: Bearer {accessToken}`
- 请求体：`file` (MultipartFile), `type` (avatar | chat)
- 响应：`{ id, url, fileName, fileSize, width, height }`
- 限制：单文件最大 5MB（压缩后），仅 JPEG/PNG/WebP

#### 3.2.2 图片访问 API
- `GET /api/images/{userId}/{fileName}`
- 权限：仅图片所属用户或公开类型可访问

### 3.3 数据隔离增强

#### 3.3.1 现有 API 改造
- `GET /api/ai/sessions` → 增加 user_id 过滤
- `GET /api/ai/sessions/{chatId}/messages` → 验证会话归属当前用户
- `DELETE /api/ai/sessions/{chatId}` → 验证会话归属当前用户
- `GET /api/ai/love_app/chat/sse` → 关联 user_id 到聊天记忆
- `GET /api/ai/manus/chat` → 关联 user_id 到代理记忆

---

## 4. 非功能需求

### 4.1 安全
- 密码使用 BCrypt 哈希（强度因子 ≥ 10）
- Access Token 有效期 30 分钟
- Refresh Token 有效期 7 天（"记住我"为 30 天）
- 所有认证相关 API 实施速率限制（5次/分钟 登录尝试）
- 文件上传路径遍历防护（已有基础）
- CORS 收紧：仅允许前端域名（开发时 localhost:5173）

### 4.2 性能
- 图片压缩在客户端完成，不增加服务端负担
- JWT 验证为内存操作，无数据库查询（Access Token）
- 图片上传响应时间 < 2s（压缩后 5MB 以内）

### 4.3 兼容性
- 前端图片压缩使用 Web Worker，不阻塞 UI
- 支持现代浏览器（Chrome 90+, Firefox 90+, Safari 15+）
- 移动端响应式适配

---

## 5. 不做什么（MVP 范围外）

- OAuth 第三方登录（Google/GitHub/微信）— 后续迭代
- 邮箱验证/激活流程 — 后续迭代
- 忘记密码/密码重置 — 后续迭代
- 用户角色/权限管理 — 后续迭代
- 图片 AI 理解（多模态）— 依赖模型能力，本次仅做上传存储
- 实时头像同步（WebSocket）— 刷新即可
