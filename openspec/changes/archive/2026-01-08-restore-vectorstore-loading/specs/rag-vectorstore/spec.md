# Spec Delta: rag-vectorstore

## ADDED Requirements

### Requirement: VectorStore loads classpath documents asynchronously with resilience
The system SHALL populate the LoveApp vector store with documents from `classpath:document/*.md` after application startup without blocking the main startup sequence, and SHALL tolerate transient embedding failures via bounded retries.

#### Scenario: Startup with reachable embedding service
- **WHEN** the application starts with `app.rag.vectorstore.load-mode=async`
- **THEN** a background task SHALL load the markdown documents and add them to the vector store, logging a success message with the document count.

#### Scenario: Startup when embedding service is unreachable
- **WHEN** the embedding provider is unreachable during the background load
- **THEN** the system SHALL log a warning, retry up to the configured max attempts with backoff, and SHALL NOT fail the application startup.

#### Scenario: Load is intentionally disabled
- **WHEN** `app.rag.vectorstore.load-mode=off`
- **THEN** the system SHALL skip loading documents into the vector store and log that document loading is disabled.
