# HOST_USAGE_GUIDE

## Trigger Modes
- Chat mode: describe requirement directly
- Terminal mode: run backend/frontend commands

## Workflow Gate Reminder
- Clarify requirement scope before coding
- Keep design and implementation changes traceable in repository files

## Smoke
1. Run backend in local profile.
2. Run frontend dev server.
3. Verify main chat flow loads and requests reach `/api`.

## Common Recovery
- Backend build fails: run `mvn -DskipTests=true package` and inspect error logs.
- Frontend build fails: run `cd springai-front-react && npm run lint && npm run build`.
