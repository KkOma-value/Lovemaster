## Context
Lovemaster already has two orchestration flows, `LoveChatOrchestrator` and `CoachChatOrchestrator`, that call a shared `RagKnowledgeService`. This makes the RAG integration point narrow and allows the approved Dify migration to remain small and isolated.

The repository still contains local vector-store infrastructure used by older experiments and helper methods. The user explicitly asked to remove the local vector-store fallback from the main backend flow, but that does not require deleting every historical class in one change.

## Goals / Non-Goals
- Goals:
  - Use Dify dataset retrieval as the main knowledge source for Love and Coach orchestration.
  - Preserve the existing SSE events and orchestrator call order.
  - Fail open when Dify is unavailable by continuing without injected knowledge.
  - Stop default local vector-store loading for the main path.
- Non-Goals:
  - Refactor the agent inheritance chain.
  - Replace DashScope chat generation.
  - Remove all legacy vector-store code from the repository.

## Decisions
- Decision: Introduce a dedicated `DifyKnowledgeService` under `ai.service`.
  - Why: it isolates external API concerns from orchestration and keeps `RagKnowledgeService` as the stable facade already used by both flows.
- Decision: Use Spring `RestClient` with explicit connect/read timeouts.
  - Why: it is already available in the current stack without adding a new web dependency.
- Decision: Return an empty string on missing config, API failure, timeout, or empty result.
  - Why: the approved architecture requires graceful degradation without blocking the main answer path.
- Decision: Disable default vector-store document loading in configuration rather than removing all vector-store beans.
  - Why: this avoids wider regressions in unrelated legacy methods while removing the main-path dependency.

## Risks / Trade-offs
- Dify requires both an API key and a dataset ID.
  - Mitigation: configuration keeps them separate; missing values log a warning and fail open.
- External API contract drift could break parsing.
  - Mitigation: response DTOs ignore unknown fields and only depend on required nested content fields.
- Legacy methods may still rely on local vector-store classes.
  - Mitigation: retain those classes for now, but stop default loading and remove them from the main RAG path.

## Migration Plan
1. Add Dify config + client + DTOs.
2. Switch `RagKnowledgeService` to Dify-backed retrieval.
3. Set vector-store load mode default to `off`.
4. Update templates/tests and validate with targeted Maven runs.

## Open Questions
- The provided user input confirms the dataset API key, but the concrete Dify dataset ID still must be configured separately for live retrieval.
