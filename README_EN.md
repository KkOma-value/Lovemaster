# Lovemaster

## Run Locally
1. Start backend in repository root:
	- `mvn spring-boot:run -Dspring-boot.run.profiles=local`
2. Start React frontend:
	- `cd springai-front-react`
	- `npm install`
	- `npm run dev`

## Build And Test
- Backend tests: `mvn test`
- Backend package: `mvn -DskipTests=true package`
- Frontend lint: `cd springai-front-react && npm run lint`
- Frontend build: `cd springai-front-react && npm run build`
