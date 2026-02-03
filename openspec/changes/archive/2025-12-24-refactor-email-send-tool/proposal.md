# refactor-email-send-tool

## Summary
Pure refactor of `EmailSendTool` to make the existing text-only email sending capability reliably usable and configurable by default, without changing its boolean return contract.

## Motivation
- Duplicate/overlapping configuration between `MailConfig` and `EmailSendTool.ensureMailSenderConfigured()` risks misconfiguration (starttls vs SSL, auth, debug flags).
- The tool is not registered in `ToolRegistration`, so agents cannot call it by default despite existing implementation.
- Logging uses `System.out/err` and forces `mail.debug=true`, which can expose credentials and adds noise.
- Need a clean, single-source mail configuration and Spring-managed wiring while keeping behavior (text email, boolean return) unchanged.

## Scope
- Keep `sendEmail(to, subject, content) -> boolean` signature and semantics (true on send success, false on failure) unchanged.
- Limit to plain text email (no HTML, attachments, cc/bcc, templating).
- Default availability: register the tool in the global tool set so agents can invoke it.
- Centralize mail sender configuration (one source of truth) and remove redundant in-method configuration.
- Improve logging/observability without changing outward behavior (e.g., switch to logger, avoid leaking secrets, keep return values the same).

## Non-Goals
- No new email features (HTML, attachments, cc/bcc, templates, rate limiting).
- No change to public method signature or return type.
- No new configuration surface beyond existing `spring.mail.*` keys unless needed for parity.

## Risks / Mitigations
- Risk: Changing configuration path may break existing deployments. Mitigation: honor existing `spring.mail.*` keys; keep defaults compatible with current QQ SMTP settings.
- Risk: Registering tool globally could enable unintended use. Mitigation: document expectation and keep behavior identical; no additional side effects.

## Dependencies / Open Questions
- None pending; scope is confirmed as pure refactor, text-only, default availability.

## Plan
- Align mail sender configuration to a single source (Spring config) and remove redundant runtime config in the tool.
- Ensure the tool is Spring-managed and registered in `ToolRegistration` without bypassing DI.
- Harden logging/observability (use logger, no credential leakage) while preserving boolean outcomes.
- Validate by running targeted checks (compile, minimal dry-run/unit test if feasible) without altering API/behavior.
