# Change: Replace Primary RAG Retrieval with Dify Knowledge API

## Why
The current Love and Coach orchestration flows retrieve knowledge from a local in-memory vector store. This does not match the approved backend architecture, is harder to maintain, and depends on local embedding/loading behavior the user explicitly wants to remove from the main path.

## What Changes
- Add a Dify-backed retrieval service for the approved `rewrite -> rag -> brain` pipeline.
- Update backend RAG orchestration to use Dify retrieval for both Love and Coach flows.
- Disable default local vector-store loading for the main path so Dify becomes the primary retrieval mechanism.
- Keep retrieval failures non-fatal by returning an empty knowledge context instead of blocking the request.

## Impact
- Affected specs: `dify-rag-retrieval`
- Affected code: `src/main/java/org/example/springai_learn/ai/**`, `src/main/resources/application*.yml`, related tests
