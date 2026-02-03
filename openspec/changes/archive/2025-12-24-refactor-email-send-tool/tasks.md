# Tasks

- [x] Review current mail config and tool wiring; confirm existing `spring.mail.*` defaults and QQ SMTP settings are preserved.
- [x] Centralize JavaMailSender configuration: rely on `MailConfig` (or Spring Boot auto-config) as single source; remove in-method reconfiguration while keeping parity for SSL/auth/timeout defaults.
- [x] Wire `EmailSendTool` via Spring DI and register it in `ToolRegistration` so it is available to agents by default (no manual `new` bypassing DI).
- [x] Replace `System.out/err` logging with structured logger; ensure no credentials are logged; keep `sendEmail` boolean semantics unchanged.
- [x] Add minimal validation/observability (e.g., debug flag optional, safe failures); ensure text-only flow unchanged.
- [x] Validation: Modified files (EmailSendTool, MailConfig, ToolRegistration) have no compile errors. Pre-existing build failures in other files (ReReadingAdvisor, TabooWordAdvisor, KkomaManus, TerminateTool) are unrelated to this refactor.
