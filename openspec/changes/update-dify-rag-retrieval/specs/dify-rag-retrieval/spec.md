## ADDED Requirements
### Requirement: Love and Coach orchestration SHALL retrieve knowledge from Dify
The system SHALL use the Dify knowledge-base retrieval API as the primary knowledge source for Love and Coach orchestration after the intake rewrite step, and SHALL format the retrieved chunks into the existing injected knowledge block format.

#### Scenario: Love mode injects retrieved Dify knowledge
- **WHEN** Love mode finishes rewriting a user question and Dify returns one or more matching records
- **THEN** the system SHALL join the retrieved segment contents with `\n---\n`
- **AND** inject that formatted knowledge into the downstream brain prompt context

#### Scenario: Coach mode passes retrieved Dify knowledge into routing
- **WHEN** Coach mode finishes rewriting a user question and Dify returns one or more matching records
- **THEN** the system SHALL pass the formatted Dify knowledge into the routing decision step before deciding whether tools are needed

### Requirement: Dify retrieval failures SHALL not block answer generation
The system SHALL fail open when Dify retrieval is unavailable, misconfigured, times out, or returns no records, and SHALL continue the request without injected knowledge context.

#### Scenario: Missing Dify configuration
- **WHEN** the Dify dataset key or dataset ID is missing at runtime
- **THEN** the retrieval service SHALL log a warning
- **AND** return an empty knowledge string without throwing to the orchestrator

#### Scenario: Dify API error or timeout
- **WHEN** the Dify retrieve API returns an error response or exceeds the configured timeout
- **THEN** the retrieval service SHALL return an empty knowledge string
- **AND** the orchestrator SHALL continue generating a response

### Requirement: Main-path local vector-store loading SHALL be disabled by default
The system SHALL stop default startup-time local vector-store document loading for the approved Dify-first backend path, so the primary Love and Coach flows do not depend on local embedding/index loading.

#### Scenario: Application starts with default configuration
- **WHEN** the application starts without overriding the vector-store load mode
- **THEN** local vector-store document loading SHALL remain disabled by default
- **AND** the primary orchestration path SHALL still be able to answer requests through Dify-backed retrieval or empty-context degradation
