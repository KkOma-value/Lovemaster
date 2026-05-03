# Docker Deployment Design (Frontend + Backend, Supabase External)

Date: 2026-05-03
Owner: GitHub Copilot

## Context

The project runs a Spring Boot backend on port 8088 with SSE endpoints under /api, and a React/Vite frontend built to static assets. The database is Supabase (external). The goal is a one-click Docker Desktop startup for frontend + backend, with the frontend container proxying /api to the backend container.

## Goals

- Provide a single docker-compose command to bring up frontend and backend.
- Serve the React build via Nginx and reverse-proxy /api to the backend container.
- Keep database external (Supabase). No database container.
- Keep secrets out of the repo by using environment variables or a local .env file.
- Preserve SSE behavior by disabling proxy buffering in Nginx.

## Non-Goals

- Running Supabase locally or provisioning external infrastructure.
- Creating an all-in-one monolith container.
- Changing application code or API paths.

## Architecture Overview

- frontend: Nginx static server serving the React build and proxying /api to backend.
- backend: Spring Boot application (JAR) running on port 8088.
- database: Supabase external service accessed via JDBC (DB_POOLER_URL or DB_URL).

## Components

### Frontend Container

- Build stage: Node.js to run `npm ci` and `npm run build`.
- Runtime stage: Nginx serving `dist/`.
- Nginx config:
  - / -> static files
  - /api -> proxy_pass to backend service
  - SSE support: disable buffering, increase read timeout

### Backend Container

- Build stage: Maven builds Spring Boot JAR.
- Runtime stage: JRE 21 running the JAR.
- Environment variables set via compose (.env).
- APP_FILE_SAVE_DIR set to a mounted volume (shared tmp).

## Networking and Ports

- docker-compose default network; services communicate by name.
- Expose frontend on host port 8080 (configurable).
- Backend exposed optionally on host port 8088 (for debugging/health); internal service always on 8088.

## Configuration and Secrets

- Use a local .env file (not committed) to pass secrets:
  - DB_POOLER_URL / DB_URL, DB_USERNAME, DB_PASSWORD
  - DASHSCOPE_API_KEY, NVIDIA_API_KEY, NVIDIA_BASE_URL
  - DIFY_DATASET_KEY / DIFY_DATASET_ID (if used)
  - SUPABASE_URL / SUPABASE_SERVICE_ROLE_KEY (if used)
  - APP_FILE_SAVE_DIR
- Provide an .env.example with placeholders.

## Data Flow

1. Browser loads frontend from Nginx.
2. Frontend calls /api (same origin).
3. Nginx proxies /api to backend service over Docker network.
4. Backend streams SSE responses without buffering.

## Error Handling and Resilience

- Nginx returns 502 if backend is down; use compose to restart backend.
- Backend relies on Supabase availability; connection failures surface in logs.

## Observability and Health Checks

- Backend health endpoint: /api/health.
- Optional Docker healthcheck for backend (curl /api/health).

## Security Considerations

- No secrets baked into images.
- Backend port exposure is optional; can be disabled in compose for production.
- JWT_SECRET should be provided in .env for production.

## Testing and Validation

- `docker compose up -d --build`
- Open http://localhost:8080
- Verify SSE endpoints via UI and /api/health

## Planned File Changes

- docker-compose.yml (root)
- Dockerfile (root, backend)
- springai-front-react/Dockerfile
- springai-front-react/nginx.conf
- .env.example (root)
- .dockerignore (root)
- springai-front-react/.dockerignore (optional)
