## MODIFIED Requirements

### Requirement: Frontend SHALL connect to backend chat streaming and runtime state

The system SHALL be able to maintain chat generation state across frontend route changes and connect the UI to backend streaming or runtime recovery mechanisms for LoveApp and Coach chats.

#### Scenario: LoveApp chat remains active across route changes

- **WHEN** the frontend starts a LoveApp chat generation for a given `chatId`
- **THEN** the application SHALL keep the active generation state outside the `ChatPage` component lifecycle
- **AND** navigating away from the chat page within the same SPA session SHALL NOT implicitly terminate the generation

#### Scenario: Coach chat remains active across route changes

- **WHEN** the frontend starts a Coach chat generation for a given `chatId`
- **THEN** the application SHALL keep the active generation state outside the `ChatPage` component lifecycle
- **AND** navigating away from the chat page within the same SPA session SHALL NOT implicitly terminate the generation

#### Scenario: User re-enters a chat with an active generation

- **WHEN** the user returns to a chat that still has an active run
- **THEN** the frontend SHALL restore the running state for that chat
- **AND** it SHALL resume rendering from application runtime state and/or backend recovery state
