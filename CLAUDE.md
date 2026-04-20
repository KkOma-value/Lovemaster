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
