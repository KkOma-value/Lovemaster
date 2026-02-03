# Proposal: Restore document loading into LoveApp VectorStore without startup failure

## Background / Problem
- Current `LoveAppVectorStoreConfig` was made asynchronous to avoid startup failure when DashScope embeddings are unreachable. However, the app now may start without ever loading the classpath documents (`src/main/resources/document/*.md`), so features depending on the vector store become unusable.
- Original synchronous loading called DashScope during bean creation; when SSL handshake fails the whole application fails to start.
- We need both: documents loaded into the vector store, while startup stays non-blocking and resilient to transient embedding outages.

## Goals
- Ensure the LoveApp vector store eventually contains documents from `classpath:document/*.md` after startup.
- Avoid application start failure when the embedding service (DashScope) is unavailable or slow.
- Provide observability (logs) and a minimal switch to control load behavior.

## Non-Goals
- No change to document content or loader logic (Markdown parsing stays the same).
- No change to choice of embedding model or external provider.
- No full-blown job scheduler; only minimal retry/backoff is in scope.

## User Scenarios
1) **Normal startup with network**: App starts, schedules document loading, embeddings succeed, vector store is populated, app usable.
2) **Startup when DashScope down/unreachable**: App starts successfully; document load retries in background with logging; once network recovers, vector store populates without restart.
3) **Local/dev wants to disable load**: Optional toggle to skip background loading (e.g., offline dev) while keeping startup unaffected.

## Proposed Approach
- Introduce a small loader component triggered after `ApplicationReadyEvent` that performs document loading and embedding outside bean creation.
- Add bounded retry with backoff (e.g., limited attempts) so transient SSL/handshake errors do not require manual restart.
- Add a config flag `app.rag.vectorstore.load-mode` (values: `async` default, `off`), keeping implementation minimal; `async` triggers background load with retries; `off` skips loading.
- Emit clear logs for start, success (docs count), and failure (last error message).

## Alternatives Considered
- **Keep synchronous load and swallow errors**: Still risks long startup delays and blocks the main thread.
- **Persist embeddings to disk and reuse**: Out of scope for now (needs format/versioning/cleanup).
- **Lazy on-demand load on first query**: More invasive; on-demand might impact first user request latency.

## Risks / Open Questions
- Retry policy bounds: propose finite attempts with backoff; confirm acceptable (e.g., 3 attempts, exponential backoff up to ~1–2 minutes total).
- Should there be a manual "force reload" endpoint/command? (Out of scope unless requested.)

## Validation Plan (apply stage)
- Unit test loader behavior: honors `off` mode, triggers retries on failure, logs success once embeddings succeed (mock embedding model).
- Manual: start app without network → confirm startup succeeds and logs retry warnings; restore network → confirm success log and vector store usable.
