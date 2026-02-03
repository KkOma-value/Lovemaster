<!-- OPENSPEC:START -->
# OpenSpec Instructions

These instructions are for AI assistants working in this project.

Always open `@/openspec/AGENTS.md` when the request:
- Mentions planning or proposals (words like proposal, spec, change, plan)
- Introduces new capabilities, breaking changes, architecture shifts, or big performance/security work
- Sounds ambiguous and you need the authoritative spec before coding

Use `@/openspec/AGENTS.md` to learn:
- How to create and apply change proposals
- Spec format and conventions
- Project structure and guidelines

Keep this managed block so 'openspec update' can refresh the instructions.

<!-- OPENSPEC:END -->

# Agent Guide (SpringAI_Learn-master)

This repository contains:
- **Spring Boot backend** at repo root (Java 21, Maven)
- **MCP server** Spring Boot app in `mcp-servers/` (Java 21, Maven)
- **React front-end** in `springai-front-react/` (Vite + ESLint)
- **Vue front-end** in `springAI-front/` (Vite)

Also note: the repo uses **OpenSpec** for planning larger changes; see `openspec/AGENTS.md`.

---

## Quick Start

### Backend (root)
- Config file: `src/main/resources/application-local.yml` (gitignored)
  - Copy from `src/main/resources/application-local.yml.example` if needed.
- Default server: `http://localhost:8088/api` (see `src/main/resources/application.yml`)

### MCP Servers
- Separate Spring Boot app: `mcp-servers/`
- Default port: `8127` (see `mcp-servers/src/main/resources/application.yml`)

### Frontends
- Vue dev server: `http://localhost:3000` proxies `/api` -> `http://localhost:8088`
- React dev server: `http://localhost:5173` proxies `/api` -> `http://localhost:8088`

---

## Build / Lint / Test Commands

### Maven (repo root Spring Boot app)

**Install + build (skip tests)**
- `mvn -DskipTests=true package`

**Run app**
- `mvn spring-boot:run -Dspring-boot.run.profiles=local`

**Run all unit tests**
- `mvn test`

**Run a single test class (Surefire)**
- `mvn -Dtest=ToolCallAgentTest test`

**Run a single test method**
- `mvn -Dtest=ToolCallAgentTest#someMethodName test`

**Run tests matching pattern**
- `mvn -Dtest='*VectorStore*' test`

**Common flags**
- `-DskipITs` if integration tests are added later
- `-DtrimStackTrace=false` for debugging

### Maven (mcp-servers module)

From `mcp-servers/`:
- Build: `mvn -DskipTests=true package`
- Run: `mvn spring-boot:run -Dspring-boot.run.profiles=local`
- Test: `mvn test`
- Single test: `mvn -Dtest=ImageSearchToolTest test`

### Java “lint” / formatting
There is **no Checkstyle/SpotBugs/PMD** configured in the Maven POMs.
Preferred approach:
- Use IDE formatting (IntelliJ “Reformat Code”) and keep diffs minimal.
- Do not add new linters/formatters unless asked.

### Node (React frontend)
From `springai-front-react/`:
- Install: `npm install`
- Dev: `npm run dev`
- Build: `npm run build`
- Lint: `npm run lint` (ESLint flat config in `springai-front-react/eslint.config.js`)

### Node (Vue frontend)
From `springAI-front/`:
- Install: `npm install`
- Dev: `npm run dev`
- Build: `npm run build`

---

## Code Style Guidelines

### General
- Keep changes **small and focused**; avoid drive-by refactors.
- Prefer **clarity over cleverness**; this is a learning/demo project.
- Avoid committing secrets:
  - Put keys in `src/main/resources/application-local.yml` (gitignored) or env vars.
  - Do not hardcode tokens in `application.yml` or `src/main/resources/mcp-servers.json`.

### Java

**Version / ecosystem**
- Java: **21** (see `pom.xml`)
- Testing: **JUnit 5** (via `spring-boot-starter-test`)
- Boilerplate: **Lombok** is used; prefer Lombok annotations where consistent.

**Packages & naming**
- Base package: `org.example.springai_learn`
- Classes: `PascalCase` (e.g., `ResourceDownloadTool`)
- Methods/fields: `camelCase`
- Constants: `UPPER_SNAKE_CASE`
- Prefer descriptive names over abbreviations (`chatSessionId` vs `sid`).

**Imports**
- No wildcard imports.
- Order:
  1) `java.*` / `javax.*`
  2) third-party
  3) `org.example.*`
  4) static imports last

**Formatting**
- Use 4-space indentation.
- Keep lines reasonably short (target ~120 chars).
- Prefer explicit braces (even for single-line blocks).

**Spring conventions**
- Prefer constructor injection; keep fields `final` when possible.
- Use `@ConfigurationProperties` for larger config objects; otherwise `@Value` is ok.
- Keep controllers thin; move logic into services/tools/agents.

**Error handling**
- Validate inputs early; return clear error messages.
- For REST endpoints: prefer consistent HTTP status codes via `ResponseEntity` or `@ControllerAdvice`.
- Don’t swallow exceptions; either handle meaningfully or rethrow with context.
- When calling external systems (HTTP, filesystem, MCP):
  - Timeouts and retries should be explicit.
  - Log actionable context (request id/chat id) but never log secrets.

**Logging**
- Use `slf4j` (`log.info/debug/warn/error`) rather than `System.out`.
- Keep logs structured: include identifiers (chatId, toolName) when relevant.

**Tests**
- Place tests under `src/test/java` mirroring package structure.
- Prefer JUnit 5 + AssertJ (available via Spring Boot test starter).
- Keep tests deterministic; avoid requiring network access unless explicitly intended.

### Frontend (React)
- ESLint is configured; keep the code passing `npm run lint`.
- Prefer functional components and hooks.
- Avoid unused variables (ESLint rule allows leading `A-Z_` ignore pattern).
- Keep API base paths aligned with Vite proxy (`/api`).

### Frontend (Vue)
- No explicit lint config in the repo; follow standard Vue 3 Composition API style.
- Keep the SSE behavior intentional (avoid auto-reconnect loops).

---

## OpenSpec (when to create proposals)

If the request mentions **proposal/spec/plan**, introduces **new capabilities**, **breaking changes**, or large architecture/performance/security shifts:
- Read `openspec/AGENTS.md` and follow the OpenSpec workflow.
- Do not implement until the proposal is approved.
