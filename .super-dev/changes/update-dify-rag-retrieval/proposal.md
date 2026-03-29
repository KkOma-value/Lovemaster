# Proposal: 用 Dify Knowledge Retrieval 替换主链路本地 RAG

## 变更 ID
`update-dify-rag-retrieval`

## 状态
`approved` — 用户已确认 research / prd / architecture / uiux，并明确要求继续按 Super Dev 流程开发。

## 概述
为 Lovemaster 后端主链路接入 Dify Knowledge Base Retrieve API，让 Love 与 Coach 两条编排链都按 `rewrite -> rag dify -> brain` 执行，并移除主链路对本地 VectorStore 降级回退的依赖。

## 确认决策
1. **架构方向**：符合用户预期，继续推进 Dify RAG 集成。
2. **密钥确认**：用户确认 `dataset-...` 为 retrieve 用密钥。
3. **回退策略**：移除主链路本地 VectorStore 回退，不再把它作为 Dify 失败时的备用检索源。
4. **范围控制**：当前仅做后端优化，不追加其它方向。

## 参考文档
- `output/backend-optimize-research.md`
- `output/backend-optimize-prd.md`
- `output/backend-optimize-architecture.md`
- `output/backend-optimize-uiux.md`
