## ADDED Requirements

### Requirement: Chat generation SHALL have an explicit run lifecycle

The system SHALL model each chat generation request as a run with its own lifecycle, separate from any single page component or SSE observer.

#### Scenario: A run is created for a new generation request

- **WHEN** the user starts a LoveApp or Coach chat generation
- **THEN** the system SHALL create or register a run associated with the target `chatId`
- **AND** the run SHALL have a machine-readable status

#### Scenario: A run continues after page-level observer changes

- **WHEN** the user navigates away from the chat page while the run is still active
- **THEN** the run SHALL remain active independently of the chat page component lifecycle
- **AND** the system SHALL preserve enough state to determine whether the run is still running, completed, failed, or canceled

### Requirement: Backend SHALL expose active run state

The system SHALL provide backend capabilities to inspect active or recently finished chat runs for the current user.

#### Scenario: Client restores active runs after app navigation or reload

- **WHEN** the client initializes or restores chat runtime state
- **THEN** it SHALL be able to query backend run state for the current user
- **AND** it SHALL receive enough information to associate each run with a `chatId`, `chatType`, and status

#### Scenario: Client inspects a completed run

- **WHEN** a run has already completed before the user re-enters the chat
- **THEN** the system SHALL expose a terminal run status
- **AND** the chat history or run result SHALL remain recoverable

### Requirement: Orchestrators SHALL publish run state transitions

The system SHALL update run state as LoveApp and Coach orchestrators advance through execution.

#### Scenario: Run starts and finishes successfully

- **WHEN** an orchestrator begins processing a chat run
- **THEN** the run SHALL transition to a running state
- **AND** when processing completes successfully the run SHALL transition to a completed state

#### Scenario: Run fails

- **WHEN** an orchestrator encounters an unrecoverable error during chat generation
- **THEN** the run SHALL transition to a failed state
- **AND** the failure SHALL be queryable by the client
