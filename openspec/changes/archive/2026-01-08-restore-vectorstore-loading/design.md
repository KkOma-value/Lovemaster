# Design: Resilient LoveApp vector store loading

## Current State
- `LoveAppVectorStoreConfig` creates a `SimpleVectorStore` bean and (after recent change) defers document loading to an async `ApplicationReadyEvent` listener.
- Document loader reads `classpath:document/*.md` via `LoveAppDocumentLoader` and produces a list of `Document` objects.
- When embedding provider (DashScope) is unreachable, the vector store remains empty and there is no retry; previously startup would fail synchronously.

## Proposed Architecture
- Create a dedicated `LoveAppVectorStoreLoader` (component or service) that:
  - Triggers on `ApplicationReadyEvent` (only once).
  - Reads documents via `LoveAppDocumentLoader`.
  - Invokes `VectorStore.add()` in a background task.
  - Implements bounded retry with backoff (configurable defaults, e.g., 3 attempts, 2x backoff starting at 5s).
  - Logs start/success/failure.
- Configuration flag `app.rag.vectorstore.load-mode`:
  - `async` (default): run background load with retry.
  - `off`: skip load entirely (log info).
- Avoid circular dependency by obtaining the vector store via the `ApplicationContext` or constructor injection of the bean itself (after it is fully created); the loader should not be a `@Configuration` class.

## Data Flow
1. App starts → `VectorStore` bean is constructed without loading documents.
2. `ApplicationReadyEvent` fires → loader checks `load-mode`.
3. If `async`: spawn background task → load documents → call `vectorStore.add(documents)`.
4. On failure: log warning and retry until attempts exhausted; give final warning.
5. On success: log document count.

## Config & Observability
- Property: `app.rag.vectorstore.load-mode` (default `async`).
- Property: `app.rag.vectorstore.load.max-attempts` (default 3) and `backoff-seconds` (default 5, exponential).
- Logs:
  - Start message with attempt number and doc count to load.
  - Success message with total docs added.
  - Warning on failure with exception message and next backoff.

## Edge Cases
- No documents found: log info, do not treat as error.
- Embedding model throws: count as failure for retry.
- Loader should be idempotent; avoid duplicate loads (track a flag or rely on single-run attempts).

## Validation
- Unit tests with mock `VectorStore` and `LoveAppDocumentLoader` to cover:
  - `async` mode success path.
  - `off` mode does nothing.
  - Retry stops after max attempts when all fail.
  - Success after interim failure populates store once.
- Manual: disable network to DashScope → see retry logs; enable network → see success log and search works.
