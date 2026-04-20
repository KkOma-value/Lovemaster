# HOST_RUNTIME_VALIDATION

## host runtime validation

## Research Phase Behavior
- Ensure the host can complete build, run, and test flows in current repository state.

## Validation Steps
1. Start backend with `mvn spring-boot:run -Dspring-boot.run.profiles=local`.
2. Start frontend with `cd springai-front-react && npm run dev`.
3. Verify `/api/health` and frontend proxy behavior.
4. Run `mvn test` and `cd springai-front-react && npm run build`.
