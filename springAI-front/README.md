# SpringAI 前端项目（恋爱教练 / 脱单助手）

这是一个基于 Vue3 + Element Plus 的前端聊天应用，用于与 SpringBoot 后端的 AI 接口进行交互。

## 技术栈

- **Vue 3** - 前端框架
- **Element Plus** - UI 组件库
- **Axios** - HTTP 请求库
- **Vite** - 构建工具
- **Server-Sent Events (SSE)** - 实时流式数据传输

## 功能特性

- 支持两种“恋爱场景”对话入口
  - 脱单助手（Love App，支持 chatId）
  - 恋爱教练（Manus）
- 💬 实时流式聊天响应
- 🎨 现代化的聊天界面
- 📱 响应式设计
- 🔄 自动滚动到最新消息
- 🧹 一键清空聊天记录

## 后端接口

项目对接以下 SpringBoot 后端接口：

### 1. Love App 聊天接口
```
GET /api/ai/love_app/chat/sse
参数:
- message: 聊天消息
- chatId: 聊天会话ID
响应: text/event-stream (SSE流)
```

### 2. Manus 聊天接口
```
GET /api/ai/manus/chat
参数:
- message: 聊天消息
响应: SseEmitter (SSE流)
```

## 安装和运行

### 1. 安装依赖
```bash
npm install
```

### 2. 启动开发服务器
```bash
npm run dev
```

项目将在 http://localhost:3000 启动

### 3. 构建生产版本
```bash
npm run build
```

## 项目结构

```
src/
├── components/          # Vue 组件
│   ├── LoveAppChat.vue # Love App 聊天组件
│   └── ManusChat.vue   # Manus 聊天组件
├── views/              # 页面视图
│   ├── HomePage.vue    # 首页
│   ├── LoveAppPage.vue # Love App 聊天页面
│   └── ManusPage.vue   # Manus 聊天页面
├── router/             # 路由配置
│   └── index.js        # 路由定义
├── services/           # 服务层
│   └── api.js          # SSE 连接管理（防重连）
├── App.vue             # 根组件
└── main.js             # 应用入口
```

## 主要功能

### SSE 流式聊天
- 使用原生 EventSource API 处理 SSE 连接
- 实时显示 AI 回复的流式数据
- 收到 `[DONE]` 标记或连接完成后立即关闭，**禁止自动重连**（避免后端重复处理同一请求）

### 用户体验
- 发送消息时显示加载状态
- 实时显示 AI 正在输入的内容
- 消息时间戳显示
- 自动滚动到最新消息

### 错误处理
- 网络连接异常提示
- 后端服务不可用提示
- 用户友好的错误信息

## 配置说明

### 后端服务地址
开发模式下，Vite 会将 `/api` 请求代理到后端 `http://localhost:8088`。

如需修改后端地址，编辑 `vite.config.js` 中的 `proxy.target`：

```javascript
proxy: {
  '/api': {
    target: 'http://localhost:8088',  // 修改为你的后端地址
    changeOrigin: true
  }
}
```

### 跨域配置
开发模式使用 Vite 代理，无需担心 CORS 问题。生产部署时请确保前后端同域或后端已配置 CORS。

## 开发说明

- 项目使用 Vue 3 Composition API
- UI 组件基于 Element Plus
- 状态管理使用 Vue 3 的 reactive 系统

1. **先启动后端**：确保后端服务运行在 `http://localhost:8088`（可通过项目根目录的 `start.bat` 或 `mvn spring-boot:run -Dspring-boot.run.profiles=local` 启动）
2. **再启动前端**：`npm run dev`，访问 `http://localhost:3000`
## 注意事项

1. 确保后端服务运行在 `http://localhost:8088`
2. 后端需要支持 CORS 跨域请求
3. SSE 连接需要现代浏览器支持
4. 建议在开发时打开浏览器控制台查看连接状态 