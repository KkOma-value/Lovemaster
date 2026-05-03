# CLAUDE.md

Guidance for Claude Code when working with Lovemaster.

## Project Overview

Lovemaster is a full-stack AI application for dating companion/coaching scenarios. Built with Spring Boot 3.4.5 + Spring AI + Java 21 backend, React 19 + Vite 7 frontend.

## Quick Commands

```bash
# Backend
./start.sh                          # Quick start (or start.bat on Windows)
mvn spring-boot:run -Dspring-boot.run.profiles=local
mvn test                            # Run tests
mvn -DskipTests=true package        # Build JAR

# MCP Server
cd mcp-servers && mvn spring-boot:run -Dspring-boot.run.profiles=local

# React Frontend
cd springai-front-react
npm install && npm run dev          # Port 5173, proxies /api -> 8088
npm run lint && npm run build

# OpenSpec
openspec list                       # List active changes
openspec show [item]                # Display details
openspec validate [change] --strict # Validate
# See openspec/AGENTS.md for full workflow
```

## Architecture

### Layers
1. **Controller** (`controller/`): REST APIs + SSE streaming
2. **Agent** (`agent/`): `BaseAgent → ReActAgent → ToolCallAgent → KkomaManus`
3. **Tool** (`tools/`): `@Tool` annotated, registered in `ToolRegistration`
4. **RAG** (`rag/`): PostgreSQL + PgVector, async loading with retry
5. **Application** (`app/`): `LoveApp` — Love/Coach/Kiko AI modes

### Key Patterns
- **SSE Streaming**: `{"type":"message","content":"..."}` + `[DONE]` marker
- **Tool Registration**: Spring components, dynamic by API key presence
- **Advisor**: `LoveAppRagCustomAdvisorFactory` for filtered RAG
- **Coach Flow**: Intake → RAG → BrainAgent → (direct | ToolsAgent) → BrainAgent
- **Kiko AI**: `ProbabilityAnalysisService` → structured `ProbabilityAnalysis` card

### Chat Modes
- **Love**: Companion chat, no tools, via `LoveChatOrchestrator`
- **Coach**: Coaching with tool calling, via `CoachChatOrchestrator`
- **Kiko**: Success probability analysis with structured cards

## Configuration

- `application.yml`: Base config
- `application-local.yml`: Local overrides (gitignored, never commit)
- Env vars for secrets: `APP_MCP_*`, `AMAP_MAPS_API_KEY`, `PEXELS_API_KEY`

### MCP Env
```
APP_MCP_CLIENT_ENABLED=true        # Enable MCP client
APP_MCP_CLIENT_INITIALIZED=false   # Set true after mcp-servers build
APP_MCP_AUTOSTART=true             # Background build
APP_FILE_SAVE_DIR=                 # Shared dir for file ops
```

## API Endpoints

Prefix: `/api`
- `GET /api/ai/love_app/chat/sse` — Love mode SSE
- `GET /api/ai/manus/chat` — Coach mode SSE
- `POST /api/ai/rewrite` — Prompt optimization endpoint (invoked only when user clicks optimize button)
- `POST /api/ai/probability/analyze` — Kiko probability analysis
- `GET /api/ai/sessions/{chatId}/runs` — Background run status
- `GET /api/health` — Health check
- `GET /api/swagger-ui.html` — API docs (Knife4j)

## Code Style

### Java
- Java 21, JUnit 5, `org.example.springai_learn`
- PascalCase classes, camelCase methods, UPPER_SNAKE_CASE constants
- No wildcard imports, 4 spaces, ~120 char lines
- Constructor injection, `@ConfigurationProperties` for config
- slf4j logging with identifiers (chatId, toolName)
- Never log secrets

### React
- ESLint in `springai-front-react/eslint.config.js`
- Functional components + hooks
- Pass `npm run lint`

### Linting
- No Checkstyle/SpotBugs/PMD in POM
- Use IDE formatting, keep diffs minimal
- Don't add new linters unless asked

## Important Notes

1. **No formal test coverage** — learning project
2. **Never commit API keys** — use `application-local.yml` or env vars
3. **PostgreSQL + PgVector** required for RAG
4. **Java 21+** required
5. **RAG loading**: set `APP_RAG_LOAD_MODE=off` to disable

## OpenSpec

For proposals, specs, plans, or large architecture changes:
1. Read `openspec/AGENTS.md`
2. Follow proposal → approval → implement flow
3. Do not implement before approval

## graphify

Knowledge graph at `graphify-out/`. Rules:
- Before architecture questions: read `graphify-out/GRAPH_REPORT.md`
- If `graphify-out/wiki/index.md` exists, navigate it instead of raw files
- After code changes: run `graphify update .` (AST-only, no API cost)

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
