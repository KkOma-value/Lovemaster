## Tasks
- [x] Grounding: review current `LoveAppVectorStoreConfig` and `LoveAppDocumentLoader` behavior; confirm document source patterns.
- [x] Specs: add `rag-vectorstore` spec deltas covering async load with retry and toggleable load mode.
- [x] Design: finalize loader component responsibilities, retry/backoff defaults, and configuration keys.
- [x] Implementation: introduce loader component (non-`@Configuration`), config properties, and background loading with bounded retry; avoid circular deps.
- [x] Tests: unit tests for success, off-mode, and retry-exhausted paths (mock `VectorStore` and loader inputs).
- [x] Docs: update README with new config flags and behavior.
- [x] Validation: run `mvn test` and `openspec validate restore-vectorstore-loading --strict`.
