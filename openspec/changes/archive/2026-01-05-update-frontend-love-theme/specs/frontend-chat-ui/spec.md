# frontend-chat-ui Specification (Delta)

## ADDED Requirements

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

## MODIFIED Requirements

### Requirement: Frontend SHALL build and start
The system SHALL allow `springAI-front` to build and start without missing-module errors.

#### Scenario: Developer runs the frontend dev server
- **WHEN** a developer runs `npm install` and `npm run dev` in `springAI-front`
- **THEN** the application SHALL start successfully without “module not found” errors
- **AND** the primary UI content SHALL be love-themed by default
