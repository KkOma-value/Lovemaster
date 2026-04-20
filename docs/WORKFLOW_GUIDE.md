# WORKFLOW_GUIDE

## Standard Flow
requirement clarification -> design docs -> implementation -> frontend verification -> backend verification -> quality -> delivery

## Required Commands
- `mvn test`
- `mvn -DskipTests=true package`
- `cd springai-front-react && npm run lint`
- `cd springai-front-react && npm run build`

## Gate Rules
- Do not start coding before docs confirmation.
- Do not proceed to backend before frontend runtime evidence.
