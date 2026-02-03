# Email Tool

## ADDED Requirements

### Requirement: Email tool SHALL be registered and DI-managed
The existing text email tool SHALL be Spring-managed and exposed to agents by default without changing its public API.

#### Scenario: Agent can invoke email tool
Given the application is running with valid `spring.mail.*` settings
When an agent selects the email tool for a request
Then the tool instance is provided from Spring DI and is available in the global tool registry
And the tool method signature remains `sendEmail(to, subject, content) -> boolean`

### Requirement: Mail configuration MUST be single-sourced
Mail transport configuration MUST come from a single source of truth (Spring configuration) rather than ad-hoc reconfiguration inside the tool, while preserving current SSL/auth defaults for QQ SMTP.

#### Scenario: Mail sender uses unified configuration
Given `spring.mail.host/port/username/password` are set (with QQ SSL defaults if unspecified)
When the email tool sends a text email
Then the JavaMailSender uses the centralized configuration (auth + SSL as configured)
And no duplicate or conflicting reconfiguration is performed inside the tool

### Requirement: Logging SHALL be safe and return semantics unchanged
Logging SHALL improve observability without changing outward behavior.

#### Scenario: Text email send result
Given a valid recipient and mail configuration
When `sendEmail` is invoked
Then it returns `true` on successful send and `false` on failure (including auth or validation errors)
And logging avoids leaking credentials and uses the standard logger instead of System.out/err
And no additional features (HTML, attachments, cc/bcc) are introduced
