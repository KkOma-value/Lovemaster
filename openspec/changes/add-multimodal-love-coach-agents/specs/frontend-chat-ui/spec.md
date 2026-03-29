## MODIFIED Requirements

### Requirement: Frontend SHALL connect to backend SSE endpoints
The system SHALL be able to open SSE connections to the backend chat endpoints for LoveApp and Manus, including image-assisted requests and staged status events for screenshot understanding.

#### Scenario: LoveApp SSE stream with optional screenshot
- **WHEN** the frontend opens an SSE connection to `/api/ai/love_app/chat/sse` with `message`, optional `chatId`, and optional `imageUrl`
- **THEN** the UI SHALL display staged intake or answer-generation updates
- **AND** the UI SHALL display streamed content chunks
- **AND** the stream SHALL stop when `[DONE]` is received

#### Scenario: Manus SSE stream with optional screenshot
- **WHEN** the frontend opens an SSE connection to `/api/ai/manus/chat` with `message`, optional `chatId`, and optional `imageUrl`
- **THEN** the UI SHALL display staged intake updates before task execution
- **AND** the UI SHALL continue to handle task events until the connection completes or is closed

#### Scenario: Screenshot analysis status event handling
- **WHEN** the backend sends `thinking`, `status`, `intake_status`, `ocr_result`, or `rewrite_result` events
- **THEN** the frontend SHALL render those events as user-readable progress updates
- **AND** generic internal terminology SHALL NOT be exposed as the primary user-facing copy

## ADDED Requirements

### Requirement: Chat input SHALL support screenshot-first relationship analysis
The system SHALL present the chat input as suitable for pasting chat text or uploading screenshots in both Love and Coach modes.

#### Scenario: User prepares a screenshot for Love mode
- **WHEN** a user attaches a screenshot in Love mode
- **THEN** the input UI SHALL preview the image
- **AND** the UI SHALL communicate that the screenshot will be analyzed before generating reply suggestions

#### Scenario: User prepares a screenshot for Coach mode
- **WHEN** a user attaches a screenshot in Coach mode
- **THEN** the input UI SHALL preview the image
- **AND** the UI SHALL communicate that the system will first understand the conversation before deciding whether to execute tasks

### Requirement: Mode copy SHALL distinguish Love mode from Coach mode
The system SHALL clearly differentiate Love mode and Coach mode through page copy, helper text, and welcome content.

#### Scenario: User enters Love mode
- **WHEN** a user opens the Love mode chat page
- **THEN** the header and welcome content SHALL emphasize emotional analysis and direct reply suggestions

#### Scenario: User enters Coach mode
- **WHEN** a user opens the Coach mode chat page
- **THEN** the header and welcome content SHALL emphasize task assistance and conditional execution

### Requirement: Coach panel SHALL stay closed during ordinary analysis
The system SHALL avoid automatically opening the Coach panel for ordinary intake or answer-generation statuses.

#### Scenario: Coach mode performs screenshot understanding only
- **WHEN** Coach mode receives screenshot-related status updates without entering a task-execution phase
- **THEN** the panel SHALL remain closed
- **AND** the analysis progress SHALL remain visible in the main chat stream

#### Scenario: Coach mode enters task execution
- **WHEN** Coach mode emits task execution events such as `task_start`, `task_progress`, `terminal`, or `file_created`
- **THEN** the panel SHALL open and display the corresponding task telemetry
