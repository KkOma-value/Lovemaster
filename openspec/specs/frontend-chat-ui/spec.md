# frontend-chat-ui Specification

## Purpose
TBD - created by archiving change fix-frontend-chat-ui. Update Purpose after archive.
## Requirements
### Requirement: Frontend SHALL build and start
The system SHALL allow `springAI-front` to build and start without missing-module errors.

#### Scenario: Developer runs the frontend dev server
- **WHEN** a developer runs `npm install` and `npm run dev` in `springAI-front`
- **THEN** the application SHALL start successfully without “module not found” errors
- **AND** the primary UI content SHALL be love-themed by default

### Requirement: LoveApp chat route SHALL render
The system SHALL provide a LoveApp route that renders the LoveApp chat UI.

#### Scenario: User navigates to LoveApp page
- **WHEN** a user opens `/loveapp`
- **THEN** the page SHALL render a LoveApp chat component and provide a way to navigate back to home

### Requirement: Theme switch SHALL toggle exactly once
The system SHALL toggle theme state exactly once per user action.

#### Scenario: User toggles theme
- **WHEN** a user toggles the theme switch on the home page
- **THEN** the global theme state SHALL change to the requested value and remain consistent with the switch UI

### Requirement: Frontend SHALL connect to backend SSE endpoints
The system SHALL be able to open SSE connections to the backend chat endpoints for LoveApp and Manus.

#### Scenario: LoveApp SSE stream
- **WHEN** the frontend opens an SSE connection to `/api/ai/love_app/chat/sse` with `message` and optional `chatId`
- **THEN** the UI SHALL display streamed chunks and stop when `[DONE]` is received

#### Scenario: Manus SSE stream
- **WHEN** the frontend opens an SSE connection to `/api/ai/manus/chat` with `message`
- **THEN** the UI SHALL display streamed chunks until the connection completes or is closed

### Requirement: UI content SHALL be love-themed by default
The system SHALL present love/relationship-focused UI copy and example prompts in the primary user-facing pages.

#### Scenario: User opens the home page
- **WHEN** a user visits the home page
- **THEN** the title, subtitles, and feature highlights SHALL be aligned with a love/relationship theme
- **AND** the page SHALL NOT emphasize unrelated technical topics by default

#### Scenario: User sees example prompts
- **WHEN** a user views example prompts in chat pages
- **THEN** prompts SHALL be about love/relationship scenarios (e.g., communication, dating, reconciliation, boundaries)
- **AND** prompts SHALL NOT include unrelated technical topics (e.g., Java/Spring/Docker)

