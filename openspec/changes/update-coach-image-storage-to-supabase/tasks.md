## 1. Implementation
- [x] 1.1 对齐 `conversation_images` 数据模型与接口输出，统一使用 `public_url` / `storage_path` / `source_url`
- [x] 1.2 新增或改造云端图片上传能力，使 Coach 图片从外部 URL 直接上传到 Supabase Storage，而不是先作为持久化依赖写入本地目录
- [x] 1.3 调整 ToolsAgent 图片事件处理，只为成功上传到 Supabase Storage 的图片写入数据库并推送前端 URL
- [x] 1.4 修改会话图片查询接口，返回可直接渲染的 `public_url` 并维持会话归属校验
- [x] 1.5 修改 React Coach 面板历史恢复与预览逻辑，直接使用云端 URL 渲染图片
- [x] 1.6 补充失败场景诊断与去重逻辑，避免无效记录或重复图片

## 2. Verification
- [x] 2.1 后端执行 `mvn -DskipTests=true package`
- [x] 2.2 前端执行 `npm run lint`
- [ ] 2.3 手动验证 Coach 模式下载图片、刷新页面后图片历史回显、删除会话后云端元数据清理
