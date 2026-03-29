# Tasks: update-dify-rag-retrieval

## 实现顺序：spec / tasks 落盘 -> 后端实现 -> 定向验证 -> 交付

## Phase 1: Governance

- [x] 创建 `.super-dev/changes/update-dify-rag-retrieval/` proposal / tasks
- [x] 创建 `openspec/changes/update-dify-rag-retrieval/` proposal / tasks / design / delta specs

## Phase 2: Backend

- [x] 新增 Dify 配置绑定与 HTTP 客户端配置
- [x] 新增 Dify retrieve 请求/响应 DTO
- [x] 新增 Dify 知识检索服务并实现超时、重试、日志
- [x] 修改 `RagKnowledgeService`，主链路改为只走 Dify
- [x] 调整默认配置，避免本地 VectorStore 默认加载

## Phase 3: Validation

- [x] 更新/新增单元测试覆盖 Dify 检索与 RAG 服务
- [x] 运行 Maven 定向测试或编译验证
