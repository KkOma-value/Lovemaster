# Change: Move Coach Image Artifacts to Supabase Storage

## Why
Coach 模式当前的图片链路仍然依赖本地 `tmp/download` 落盘，再由后端补传到 Supabase 或回退到本地下载地址。这会让图片历史回显、环境迁移和跨设备访问都受本地文件系统影响，也和“前端直接使用 Supabase 图片链接渲染”的目标不一致。

## What Changes
- 将 Coach 模式的图片下载产物改为直接上传到 Supabase Storage，不再把会话图片作为持久化依赖落到本地磁盘
- 统一 `conversation_images` 的持久化字段与 API 输出，保存 `conversation_id`、`file_name`、`source_url`、`storage_path`、`public_url`、`created_at`
- 调整 ToolsAgent 的图片工具收尾逻辑，只把真正上传成功的云端图片作为会话图片资产写入数据库和 SSE `file_created`
- 调整前端 Manus 预览与历史恢复逻辑，直接渲染 Supabase `public_url`
- 补充配置与错误处理，使 Supabase Storage 未配置、上传失败、图片链接无效时返回可诊断结果

## Impact
- Affected specs: `manus-agent`, `frontend-chat-ui`
- Affected code:
  - `src/main/java/org/example/springai_learn/ai/service/ToolsAgentService.java`
  - `src/main/java/org/example/springai_learn/tools/ResourceDownloadTool.java`
  - `mcp-servers/src/main/java/org/example/mcpservers/server/ImageSearchTool.java`
  - `src/main/java/org/example/springai_learn/auth/service/SupabaseStorageClient.java`
  - `src/main/java/org/example/springai_learn/controller/ChatSessionController.java`
  - `src/main/java/org/example/springai_learn/auth/entity/ConversationImage.java`
  - `springai-front-react/src/pages/Chat/ChatPage.jsx`
  - `springai-front-react/src/components/ManusPanel/ManusPreview.jsx`
