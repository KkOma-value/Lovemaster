# Change: fix-local-startup

## Why
Local builds fail on clean machines due to a Java 21 requirement (the codebase uses Java 21 features), a missing checked-in local config template expected by `start.bat`, committed secrets in tracked config, and a compilation-blocking syntax error.

## What Changes
- Document and enforce Java 21+ for local build/run (fail fast with a clear message).
- Provide `application-local.yml.example` and ensure real local config is not committed.
- Remove hard-coded credentials/API keys from tracked config and replace with placeholders / env-var refs.
- Fix the compilation blocker in `TabooWordAdvisor.java`.
- Validate with `mvn clean compile` and a basic `spring-boot:run` using the `local` profile.

## Summary
Make the project buildable and runnable on a clean local Windows machine by clarifying prerequisites, removing hard-coded secrets from tracked config, and providing a working local configuration template.

## Motivation
- Local builds currently run under Java 8 in this environment and fail because the codebase uses Java 21 features (e.g., text blocks). The project requires Java 21+.
- `start.bat` expects `src/main/resources/application-local.yml.example`, but that file is missing, causing the script’s config check to fail.
- `src/main/resources/application.yml` and `src/main/resources/mcp-servers.json` contain hard-coded credentials/API keys. This is a security risk and prevents safe cloning/sharing.
- Some configuration properties are required at startup (`spring.ai.dashscope.api-key`, `search-api.api-key`) but are not documented as mandatory and/or are missing from tracked config, leading to confusing failures.
- There is at least one source-level syntax break in `TabooWordAdvisor.java` (string literal split across lines), which blocks compilation regardless of runtime configuration.

## Scope
- Ensure `mvn clean compile` succeeds when using Java 21.
- Ensure `start.bat` can run its config check and guide users to provide required local config.
- Remove hard-coded secrets from tracked files; switch to placeholders and/or environment-variable based configuration.
- Document required environment variables and minimum setup for local run.

## Non-Goals
- No functional feature changes to agents/tools/RAG behavior.
- No new UI or API endpoints.
- No dependency upgrades beyond what is required to restore build/run.

## Risks / Mitigations
- Risk: Users relying on committed credentials/config will break. Mitigation: provide clear migration notes and an `application-local.yml.example` template.
- Risk: Removing secrets requires key rotation. Mitigation: explicitly call out rotation steps in the tasks.

## Plan
- Fix compilation blockers and enforce Java 21 as the documented prerequisite.
- Provide a local config template + ignore real local config files.
- Replace committed secrets with safe placeholders and env-var hooks.
- Validate via `mvn clean compile` under Java 21 and a basic run using `start.bat` (check-only + run).

## Impact
- Affected specs: `project-startup` (new capability spec)
- Affected code/config: `start.bat`, `pom.xml`, `src/main/resources/application*.yml`, `src/main/resources/mcp-servers.json`, `.gitignore`, `src/main/java/org/example/springai_learn/advisor/TabooWordAdvisor.java`
