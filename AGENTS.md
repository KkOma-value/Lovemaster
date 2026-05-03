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

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **Lovemaster** (4161 symbols, 8436 relationships, 299 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> If any GitNexus tool warns the index is stale, run `npx gitnexus analyze` in terminal first.

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `gitnexus_impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `gitnexus_detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `gitnexus_query({query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `gitnexus_context({name: "symbolName"})`.

## Never Do

- NEVER edit a function, class, or method without first running `gitnexus_impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `gitnexus_rename` which understands the call graph.
- NEVER commit changes without running `gitnexus_detect_changes()` to check affected scope.

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/Lovemaster/context` | Codebase overview, check index freshness |
| `gitnexus://repo/Lovemaster/clusters` | All functional areas |
| `gitnexus://repo/Lovemaster/processes` | All execution flows |
| `gitnexus://repo/Lovemaster/process/{name}` | Step-by-step execution trace |

## CLI

| Task | Read this skill file |
|------|---------------------|
| Understand architecture / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |
| Work in the Service area (346 symbols) | `.claude/skills/generated/service/SKILL.md` |
| Work in the Controller area (46 symbols) | `.claude/skills/generated/controller/SKILL.md` |
| Work in the Mcp area (44 symbols) | `.claude/skills/generated/mcp/SKILL.md` |
| Work in the Contexts area (44 symbols) | `.claude/skills/generated/contexts/SKILL.md` |
| Work in the Tools area (32 symbols) | `.claude/skills/generated/tools/SKILL.md` |
| Work in the Chat area (31 symbols) | `.claude/skills/generated/chat/SKILL.md` |
| Work in the Services area (27 symbols) | `.claude/skills/generated/services/SKILL.md` |
| Work in the ChatMemory area (21 symbols) | `.claude/skills/generated/chatmemory/SKILL.md` |
| Work in the Advisor area (16 symbols) | `.claude/skills/generated/advisor/SKILL.md` |
| Work in the Server area (15 symbols) | `.claude/skills/generated/server/SKILL.md` |
| Work in the Hooks area (13 symbols) | `.claude/skills/generated/hooks/SKILL.md` |
| Work in the Auth area (13 symbols) | `.claude/skills/generated/auth/SKILL.md` |
| Work in the Repository area (12 symbols) | `.claude/skills/generated/repository/SKILL.md` |
| Work in the ParticleBackground area (8 symbols) | `.claude/skills/generated/particlebackground/SKILL.md` |
| Work in the Orchestrator area (8 symbols) | `.claude/skills/generated/orchestrator/SKILL.md` |
| Work in the Config area (7 symbols) | `.claude/skills/generated/config/SKILL.md` |
| Work in the Components area (4 symbols) | `.claude/skills/generated/components/SKILL.md` |
| Work in the Dto area (4 symbols) | `.claude/skills/generated/dto/SKILL.md` |
| Work in the Ui area (3 symbols) | `.claude/skills/generated/ui/SKILL.md` |
| Work in the Springai_learn area (3 symbols) | `.claude/skills/generated/springai-learn/SKILL.md` |

<!-- gitnexus:end -->
