## ADDED Requirements

### Requirement: Relationship chat SHALL support screenshot-assisted intake
The system SHALL accept either plain text or a chat screenshot as the starting point for relationship analysis.

#### Scenario: User asks for reply advice with a screenshot
- **WHEN** a user submits a question together with a chat screenshot
- **THEN** the system SHALL inspect the screenshot before generating the final analysis
- **AND** the system SHALL use the screenshot-derived context as part of the answer

#### Scenario: User asks without a screenshot
- **WHEN** a user submits a text-only relationship question
- **THEN** the system SHALL continue to work without requiring an image

### Requirement: Screenshot intake SHALL rewrite ambiguous user questions into structured analysis input
The system SHALL normalize screenshot-derived and user-provided input into a structured question for downstream reasoning.

#### Scenario: User asks “what does this mean”
- **GIVEN** the user provides a short ambiguous question and a screenshot
- **WHEN** the system performs intake analysis
- **THEN** the system SHALL derive a structured question that captures the conversation context, user intent, and uncertainty notes

#### Scenario: Screenshot text contains ambiguous fragments
- **WHEN** the system cannot confidently interpret parts of the screenshot text
- **THEN** the system SHALL preserve uncertainty markers rather than inventing missing meaning

### Requirement: Love mode SHALL produce relationship analysis and sendable reply suggestions
The system SHALL use the structured intake result to produce direct relationship advice for Love mode.

#### Scenario: Love mode explains intent and suggests replies
- **WHEN** Love mode finishes intake analysis
- **THEN** the response SHALL explain the likely meaning or emotional signal in the conversation
- **AND** the response SHALL include reply suggestions the user can directly send or adapt

#### Scenario: Love mode handles risky dynamics
- **WHEN** the structured intake indicates manipulation, abuse, coercion, or other relationship red flags
- **THEN** the response SHALL explicitly acknowledge the risk
- **AND** the system SHALL avoid framing the situation as harmless flirtation
