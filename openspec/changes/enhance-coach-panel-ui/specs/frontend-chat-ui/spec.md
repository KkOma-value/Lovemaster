## MODIFIED Requirements

### Requirement: Frontend SHALL connect to backend SSE endpoints
The system SHALL be able to open SSE connections to the backend chat endpoints for LoveApp and Manus, and correctly handle all event types including file creation notifications.

#### Scenario: LoveApp SSE stream
- **WHEN** the frontend opens an SSE connection to `/api/ai/love_app/chat/sse` with `message` and optional `chatId`
- **THEN** the UI SHALL display streamed chunks and stop when `[DONE]` is received

#### Scenario: Manus SSE stream
- **WHEN** the frontend opens an SSE connection to `/api/ai/manus/chat` with `message`
- **THEN** the UI SHALL display streamed chunks until the connection completes or is closed

#### Scenario: File created event handling
- **WHEN** the backend sends a `file_created` event during Manus task execution
- **THEN** the frontend SHALL parse the event data containing `type`, `name`, `path`, and `url`
- **AND** the file SHALL be added to the panel's file list for preview display

## ADDED Requirements

### Requirement: Manus Panel layout SHALL not obscure chat input
The system SHALL ensure that when the Manus Panel is open, the chat input area remains fully visible and accessible.

#### Scenario: User opens Manus Panel during chat
- **WHEN** a user is in coach mode and the Manus Panel is expanded
- **THEN** the chat area SHALL automatically adjust its width to accommodate the panel
- **AND** the message input box SHALL remain visible and functional at all times

#### Scenario: User closes Manus Panel
- **WHEN** a user closes the Manus Panel
- **THEN** the chat area SHALL expand to use the full available width
- **AND** the transition SHALL be smooth and non-disruptive

### Requirement: Manus Panel theme SHALL match chat interface
The system SHALL display the Manus Panel using a visual theme consistent with the main chat interface.

#### Scenario: User views terminal output in Manus Panel
- **WHEN** a user views the terminal tab in the Manus Panel
- **THEN** the terminal background SHALL use the same light pink color scheme as the chat interface
- **AND** the terminal text SHALL have sufficient contrast for readability on the light background

#### Scenario: User views task progress in Manus Panel
- **WHEN** a user views task progress in the Manus Panel
- **THEN** the task list styling SHALL be consistent with the overall pink theme
- **AND** completed, active, and pending states SHALL be visually distinct

### Requirement: Downloaded resources SHALL be previewable in Manus Panel
The system SHALL display resources downloaded by the AI agent in the Manus Panel preview tab.

#### Scenario: AI agent downloads an image
- **WHEN** the AI agent successfully downloads an image using the downloadResource tool
- **THEN** the backend SHALL send a `file_created` event with the image URL
- **AND** the frontend SHALL display the image in the Manus Panel preview tab
- **AND** the image SHALL be accessible via `/api/files/download/{fileName}`

#### Scenario: Image loading fails
- **WHEN** an image file cannot be loaded in the preview tab
- **THEN** the system SHALL display an appropriate error placeholder
- **AND** the file metadata (name, path) SHALL still be visible to the user
