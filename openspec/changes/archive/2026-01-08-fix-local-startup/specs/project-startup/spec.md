## ADDED Requirements

### Requirement: Local build prerequisites
The system SHALL document and require Java 21+ for building and running the application.

#### Scenario: Developer uses unsupported Java
- **WHEN** a developer runs the project with Java < 21
- **THEN** the startup script SHALL fail fast with a clear message indicating Java 21+ is required

### Requirement: Local configuration template
The repository SHALL provide a checked-in local configuration template for required runtime settings.

#### Scenario: Fresh clone local setup
- **WHEN** a developer clones the repository
- **THEN** the repository SHALL include `application-local.yml.example` with placeholders for required keys

### Requirement: No committed secrets
The repository SHALL NOT include hard-coded credentials or API keys in tracked configuration files.

#### Scenario: Secrets are required for runtime
- **WHEN** runtime credentials/API keys are needed
- **THEN** configuration SHALL reference environment variables or local-only config not committed to VCS

### Requirement: Project compiles cleanly
The project SHALL compile successfully with the documented prerequisite JDK.

#### Scenario: Maven compile
- **WHEN** a developer runs `mvn clean compile` under Java 21
- **THEN** compilation SHALL succeed without syntax errors
