## 1. Specification

- [x] 1.1 Confirm the non-UI scope: application runtime and backend run lifecycle only
- [x] 1.2 Validate the OpenSpec deltas for `frontend-chat-ui` and `chat-run-lifecycle`

## 2. Frontend Runtime

- [x] 2.1 Introduce an application-level chat runtime provider/store that outlives `ChatPage`
- [x] 2.2 Move active streaming connection ownership out of `ChatPage`
- [x] 2.3 Restore per-chat running state when the user navigates back into a chat
- [x] 2.4 Keep existing pages functional without implementing new UI designs

## 3. Backend Run Lifecycle

- [x] 3.1 Introduce a run model and service abstraction for active chat executions
- [x] 3.2 Expose backend endpoints to query active runs and fetch run status
- [x] 3.3 Ensure orchestrators publish run state transitions independent of page-scoped SSE observers
- [x] 3.4 Persist enough run/result state for reconnect and post-completion recovery

## 4. Validation

- [x] 4.1 Run `openspec validate add-background-chat-runs --strict`
- [x] 4.2 Run targeted frontend checks for runtime wiring
- [x] 4.3 Run targeted backend tests for run lifecycle and recovery behavior
