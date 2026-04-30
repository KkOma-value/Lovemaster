# QUICKSTART

## Goal

Provide the shortest successful path to run Lovemaster locally.

## Environment Requirements

- Java 21+
- Maven 3.6+
- PostgreSQL 12+
- Node.js 18+

## 1. Configuration

All secrets go in `src/main/resources/application-local.yml` or environment variables. **Never commit secrets to the repo.**

Copy from a template:

```bash
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
```

Then edit `application-local.yml` with your credentials:

### Database

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/your_db
    username: your_user
    password: your_password
```

### NVIDIA NIM (AI Models)

```yaml
spring:
  ai:
    openai:
      base-url: https://integrate.api.nvidia.com
      api-key: your-nvidia-api-key

nvidia:
  model:
    rewrite: qwen/qwen3.5-122b-a10b
    tools: deepseek-ai/deepseek-r1
    brain: moonshotai/kimi-k2-thinking
```

### DashScope (Alibaba AI)

```yaml
spring:
  ai:
    alibaba:
      api-key: your-dashscope-api-key
```

### Dify (RAG Knowledge Base)

```yaml
dify:
  api:
    base-url: https://api.dify.ai/v1
    dataset-key: your-dify-dataset-api-key
    dataset-id: your-dify-dataset-id
```

### Supabase (Image Storage)

```yaml
supabase:
  url: https://your-project.supabase.co
  api-key: your-supabase-service-role-key
  storage:
    bucket: conversation-images
    public-url: https://your-project.supabase.co/storage/v1/object/public/conversation-images
```

### Google OAuth

```yaml
oauth:
  google:
    client-id: your-google-client-id
```

### Other Services

```yaml
search-api:
  api-key: ${SEARCH_API_KEY:}

pexels:
  api-key: ${PEXELS_API_KEY:}
```

## 2. Start Backend

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Default endpoints:
- API: `http://localhost:8088/api`
- Swagger: `http://localhost:8088/api/swagger-ui.html`
- Health: `http://localhost:8088/api/health`

## 3. Start Frontend

```bash
cd springai-front-react
npm install && npm run dev
```

Default: `http://localhost:5173` (proxies `/api` to backend)

## 4. Start MCP Server (Optional)

```bash
cd mcp-servers
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Default: `http://localhost:8127`

## Success Criteria

- Backend health endpoint returns normal response.
- Frontend starts without runtime errors.
- Chat page loads and API proxy works.

## Failure Recovery

- If backend startup fails, run `mvn -DskipTests=true package` first and re-run.
- If frontend fails, run `npm run lint` and `npm run build` to locate build issues.
- Ensure `application-local.yml` has correct database credentials and API keys.
