## ADDED Requirements

### Requirement: Coach mode SHALL perform intake analysis before tool execution
The system SHALL analyze the user’s text or screenshot input before deciding whether to invoke Manus tools.

#### Scenario: Coach mode answers directly after intake analysis
- **GIVEN** a Coach mode request that only needs relationship analysis or reply drafting
- **WHEN** the system completes intake understanding
- **THEN** the system SHALL return a direct answer without starting a tool-execution task sequence

#### Scenario: Coach mode invokes tools only when needed
- **GIVEN** a Coach mode request that requires external search, artifact generation, or multi-step execution
- **WHEN** the system completes intake understanding
- **THEN** the system SHALL transform the request into a tool-oriented task prompt
- **AND** the system SHALL invoke Manus execution with task telemetry

### Requirement: Coach mode telemetry SHALL distinguish understanding from execution
The system SHALL keep “input understanding” telemetry separate from “task execution” telemetry.

#### Scenario: Intake analysis emits pre-execution statuses
- **WHEN** the system is extracting screenshot text, rewriting the request, or deciding execution strategy
- **THEN** the system SHALL emit user-readable statuses without implying that a tool task has already started

#### Scenario: Tool execution emits execution telemetry
- **WHEN** the system enters Manus task execution
- **THEN** the system SHALL emit task lifecycle events such as `task_start`, `task_progress`, `terminal`, or `file_created`
- **AND** those events SHALL be suitable for the Coach panel
