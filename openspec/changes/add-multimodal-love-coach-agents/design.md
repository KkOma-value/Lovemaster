## Context

Lovemaster already has two visible user modes:

- `loveapp`: relationship-focused chat
- `coach`: Manus-style task execution with a right-side panel

The frontend can upload chat screenshots and append `imageUrl` to SSE requests, but the backend ignores that parameter. This creates a product gap: users believe the AI can inspect screenshots, but the current system still behaves as text-only chat.

The requested feature is cross-cutting:

- frontend input and status UX
- backend multimodal intake
- routing between direct reply and tool execution
- privacy-sensitive handling of screenshot-derived text

## Goals / Non-Goals

- Goals:
  - Add screenshot-aware input handling for Love and Coach modes
  - Introduce a staged intake pipeline: OCR/rewrite first, response/tool routing second
  - Preserve the current app shape and reuse existing agent/tooling infrastructure
  - Keep Coach panel behavior aligned with actual task execution
- Non-Goals:
  - Full migration to Spring AI Alibaba Graph/Supervisor in this change
  - Multi-image orchestration
  - Auto-sending messages to third-party chat platforms

## Decisions

- Decision: Introduce a new capability spec `multimodal-relationship-chat`.
  - Why: screenshot intake and relationship-focused rewrite is a distinct product capability that does not belong solely to frontend or Manus execution.

- Decision: Modify `frontend-chat-ui` and `manus-agent` instead of creating duplicate frontend/manus specs.
  - Why: both already describe SSE and Coach behavior; this change evolves those requirements.

- Decision: Keep the first implementation incremental and compatible with the existing `BaseAgent` / `ToolCallAgent` / `KkomaManus` stack.
  - Why: direct framework migration would increase scope and risk before the product loop is validated.

- Decision: Frontend-first implementation stops at a preview gate before backend work continues.
  - Why: the repository’s Super Dev contract requires a preview confirmation gate between frontend and backend phases.

## Risks / Trade-offs

- OCR can misread speaker boundaries in screenshots.
  - Mitigation: explicitly model uncertainties and avoid presenting ambiguous text as certain fact.

- The frontend can expose a polished screenshot flow before the backend is fully wired.
  - Mitigation: treat frontend-first as a UX checkpoint, then gate backend implementation behind preview confirmation.

- Coach status messages can become noisy if every analysis update opens the panel.
  - Mitigation: panel auto-open remains tied to task execution events, not generic analysis states.

## Migration Plan

1. Land approved spec and task artifacts.
2. Update the React experience for mode clarity and staged intake statuses.
3. Pause for preview confirmation.
4. Wire backend multimodal intake and routing.
5. Run validation and quality gates.

## Open Questions

- Which exact DashScope model binding is already available in local profile configuration for OCR/vision calls?
- Should the first backend implementation keep OCR and semantic reasoning in one model call, or split OCR and reasoning into separate steps?
