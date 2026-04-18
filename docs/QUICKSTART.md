# QUICKSTART

## Goal
Provide the shortest successful path to run Lovemaster locally.

## Steps
0. Idea entry: `super-dev start --idea "Lovemaster delivery hardening"`
1. Backend: run `mvn spring-boot:run -Dspring-boot.run.profiles=local` in repository root.
2. Frontend: run `npm install` and `npm run dev` in `springai-front-react`.
3. Open frontend and verify chat page loads and API proxy works.

## Workflow Gate Reminder
- 当前阶段是 `research`
- Complete research before changing implementation scope.

## Success Criteria
- Backend health endpoint returns normal response.
- Frontend starts without runtime errors.
- Chat and admin review routes are reachable.

## Failure Recovery
- If backend startup fails, run `mvn -DskipTests=true package` first and re-run.
- If frontend fails, run `npm run lint` and `npm run build` to locate build issues.
