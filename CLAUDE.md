# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SpringAI_Learn is a Java-based AI application learning project built with Spring Boot 3.4.5 and Spring AI framework. It demonstrates modern AI application development patterns including real-time chat, AI agents, tool integrations, and RAG (Retrieval-Augmented Generation) implementations.

## Common Development Commands

### Build and Run
```bash
# Quick start with the provided script
./start.sh                    # Linux/Mac
start.bat                     # Windows

# Manual Maven commands
mvn clean compile            # Compile the project
mvn spring-boot:run          # Run with Maven
mvn clean package            # Build JAR file
java -jar target/SpringAI_Learn-0.0.1-SNAPSHOT.jar  # Run JAR

# With profiles
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Development Options
```bash
./start.sh --with-tests      # Run tests before starting
./start.sh --check-only      # Only check environment
./start.sh --build-only      # Only compile project
```

### Testing Commands
```bash
# Run all unit tests
mvn test

# Run a single test class
mvn -Dtest=ToolCallAgentTest test

# Run a single test method
mvn -Dtest=ToolCallAgentTest#someMethodName test

# Run tests matching pattern
mvn -Dtest='*VectorStore*' test

# Common flags
-DskipTests=true              # Skip tests during build
-DskipITs                     # Skip integration tests if present
-DtrimStackTrace=false        # Full stack trace for debugging
```

### MCP Server Module (mcp-servers/)
The MCP server is a separate Spring Boot app that builds asynchronously after main app startup:
```bash
# From mcp-servers/ directory
cd mcp-servers
mvn -DskipTests=true package  # Build MCP server
mvn spring-boot:run           # Run MCP server (default port: 8127)
mvn test                      # Run MCP server tests
mvn -Dtest=ImageSearchToolTest test  # Single test
```

### Frontend Development
```bash
# React frontend (springai-front-react/)
cd springai-front-react
npm install                   # Install dependencies
npm run dev                   # Dev server on port 5173 (proxies /api -> 8088)
npm run build                 # Production build
npm run lint                  # Run ESLint

# Vue frontend (springAI-front/)
cd springAI-front
npm install                   # Install dependencies
npm run dev                   # Dev server on port 3000 (proxies /api -> 8088)
npm run build                 # Production build
```

### OpenSpec Commands (Spec-Driven Development)
This project uses OpenSpec for spec-driven development. Key commands:
```bash
openspec list                  # List active changes
openspec list --specs          # List specifications
openspec show [item]           # Display change or spec details
openspec validate [change] --strict  # Validate changes
openspec archive <change-id> --yes   # Archive completed changes
```

See `openspec/AGENTS.md` for complete OpenSpec workflow guidance.

## High-Level Architecture

### Core Architecture Pattern
The project follows a layered architecture with clear separation of concerns:

1. **Controller Layer** (`controller/`): REST API endpoints for external communication
   - `AiController`: Handles all AI-related API endpoints
   - SSE (Server-Sent Events) for real-time streaming responses

2. **Agent Layer** (`agent/`): AI agent implementations with inheritance hierarchy:
   - `BaseAgent`: Abstract base providing lifecycle management (states: IDLE, RUNNING, FINISHED, ERROR)
   - `ReActAgent`: Extends BaseAgent with Reasoning-Acting pattern (think/act cycle)
   - `ToolCallAgent`: Extends ReActAgent with tool integration and budget control
   - `KkomaManus`: Fully autonomous agent with file-based memory and doTerminate behavior

3. **Tool Layer** (`tools/`): External tool integrations that agents can invoke
   - Tools are annotated with `@Tool` and registered in `ToolRegistration`
   - Core tools (always available): FileOperation, WebScraping, ResourceDownload, TerminalOperation, PDFGeneration, Terminate, EmailSend
   - Conditional tools (require API keys): WebSearch (Bing/SearchAPI), ImageSearch (Pexels)
   - All tools follow a consistent interface pattern for agent consumption

4. **RAG Layer** (`rag/`): Retrieval-Augmented Generation implementation
   - Uses PostgreSQL with PgVector for vector storage
   - Async document loading with exponential backoff retry (default 3 attempts)
   - `LoveAppRagCustomAdvisorFactory`: Creates filtered advisors (e.g., by status like "已婚")
   - `QueryRewriter`: Rewrites user queries for better retrieval

5. **Application Layer** (`app/`): Specific AI applications
   - `LoveApp`: Chat application supporting multiple modes (basic, RAG, tools, MCP)

### Key Design Patterns

1. **Agent Inheritance Pattern**:
   ```
   BaseAgent → ReActAgent → ToolCallAgent → KkomaManus
   ```
   - BaseAgent provides state management, step loop, memory, SSE streaming
   - ReActAgent separates reasoning (think()) from action (act())
   - ToolCallAgent adds tool callbacks, budget control (900k API / 20k tool response)
   - KkomaManus implements full autonomy with file-based persistence

2. **Tool Registration Pattern**: Centralized tool management through `ToolRegistration` class
   - Tools are Spring components automatically discovered
   - Dynamic availability based on API key presence
   - Runtime tool availability checking

3. **Advisor Pattern**: Used in RAG implementation for customizable behavior
   - `LoveAppRagCustomAdvisorFactory` creates tailored advisors
   - Separates vector storage configuration from business logic

4. **SSE Streaming**: All chat endpoints use Server-Sent Events for real-time responses
   - Consistent response format: `{"type":"message","content":"..."}`
   - `[DONE]` marker to signal completion
   - Progress tracking with task status updates

### Configuration Management

The application uses Spring profiles with the following hierarchy:
- `application.yml`: Base configuration
- `application-local.yml`: Local development overrides (not in version control)
- Environment variables for sensitive data (API keys, passwords)

Key configuration areas:
- AI model configuration (Alibaba Cloud Tongyi Qianwen via spring.ai.alibaba)
- PostgreSQL database with PgVector (spring.datasource.*)
- Mail service (QQ email via spring.mail.*)
- Search API integration (search-api.api-key)
- MCP (Model Context Protocol) client configuration (spring.ai.mcp.*)

### MCP (Model Context Protocol) Integration

The project supports MCP client for extending tool capabilities:
- Default enabled but initialized=false to avoid startup timeout
- `mcp-servers/` module builds asynchronously after main app startup
- Environment variables:
  - `APP_MCP_CLIENT_ENABLED`: Enable/disable MCP client (default: true)
  - `APP_MCP_CLIENT_INITIALIZED`: Set to true after mcp-servers build completes
  - `APP_MCP_AUTOSTART`: Control background build process (default: true)
  - `APP_FILE_SAVE_DIR`: Shared directory for file operations between main app and MCP servers

API keys from `application-local.yml` are passed as environment variables to MCP server processes:
- `amap.maps.api-key` → `AMAP_MAPS_API_KEY`
- `pexels.api-key` → `PEXELS_API_KEY`

### API Endpoints Structure

All APIs are prefixed with `/api` and include:
- `/api/ai/love_app/chat/sse`: SSE streaming chat (LoveApp)
- `/api/ai/love_app/chat/sync`: Synchronous chat (LoveApp)
- `/api/ai/manus/chat`: Agent-based chat with streaming (KkomaManus)
- `/api/health`: Health check
- `/api/swagger-ui.html`: API documentation (Knife4j enhanced)

### Frontend Integration

The project includes Vite-based frontends:
- `springAI-front/`: Original frontend
- `springai-front-react/`: React-based frontend

Frontends communicate via:
- SSE endpoints for real-time chat
- Standard REST APIs for other functions

## Code Style Guidelines

### Java Conventions
- **Version**: Java 21 required (see pom.xml)
- **Testing**: JUnit 5 via spring-boot-starter-test
- **Base Package**: `org.example.springai_learn`
- **Naming**: PascalCase classes, camelCase methods/fields, UPPER_SNAKE_CASE constants
- **Lombok**: Used for boilerplate; prefer Lombok annotations where consistent
- **No wildcard imports**
- **Indentation**: 4 spaces, target ~120 char line length
- **Spring**: Prefer constructor injection; use `@ConfigurationProperties` for larger config objects
- **Logging**: Use slf4j (log.info/debug/warn/error), include identifiers (chatId, toolName)

### Error Handling
- Validate inputs early; return clear error messages
- For REST endpoints: prefer consistent HTTP status codes via `ResponseEntity` or `@ControllerAdvice`
- When calling external systems (HTTP, filesystem, MCP): make timeouts and retries explicit
- Never log secrets; log actionable context (request id, chat id)

### Frontend Conventions
- **React**: ESLint configured in `springai-front-react/eslint.config.js`; keep code passing `npm run lint`. Prefer functional components and hooks.
- **Vue**: Follow Vue 3 Composition API style; keep SSE behavior intentional (avoid auto-reconnect loops)

### Linting/Formatting
- **No Checkstyle/SpotBugs/PMD** configured in Maven POMs
- Use IDE formatting (IntelliJ "Reformat Code") and keep diffs minimal
- Do not add new linters/formatters unless asked

## Important Notes

1. **No Unit Tests**: This is a learning/experimentation project without formal test coverage
2. **API Keys**: Never commit API keys - use environment variables or `application-local.yml`
3. **Database**: PostgreSQL with PgVector extension required for vector storage features
4. **Java Version**: Requires Java 21+ for Spring Boot 3.4.5 compatibility
5. **Memory**: AI models can be memory-intensive - allocate sufficient JVM heap space
6. **RAG Document Loading**: Async loading with configurable retry; set `APP_RAG_LOAD_MODE=off` to disable

## OpenSpec Workflow

This project uses OpenSpec for spec-driven development. For requests involving **proposals, specs, plans**, or introducing **new capabilities, breaking changes, or large architecture/performance/security shifts**:

1. Read `openspec/AGENTS.md` for complete OpenSpec workflow guidance
2. Follow the proposal creation and approval process before implementing
3. Do not implement major changes until the proposal is approved