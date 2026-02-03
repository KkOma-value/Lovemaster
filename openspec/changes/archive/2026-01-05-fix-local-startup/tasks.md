# Tasks

- [x] Confirm Java prerequisites: document and enforce Java 21+ (local dev) and ensure `start.bat` clearly fails fast when `java -version` is not 21+.
- [x] Restore compilation: fix the string literal split in `src/main/java/org/example/springai_learn/advisor/TabooWordAdvisor.java` so `mvn clean compile` passes.
- [x] Provide local config template: add `src/main/resources/application-local.yml.example` with placeholders for required keys.
- [x] Harden configuration handling:
  - [x] Remove hard-coded DB credentials from `src/main/resources/application.yml` (placeholders and/or env var refs).
  - [x] Remove hard-coded API keys from `src/main/resources/mcp-servers.json` (placeholders/env var refs).
  - [x] Ensure required properties are documented (e.g., `spring.ai.dashscope.api-key`, `search-api.api-key`).
- [x] Align docs: Created `.gitignore` to exclude local config files from version control.
- [x] Validation:
  - [x] `mvn clean compile` succeeds under Java 21.
  - [x] Local config template exists and is properly structured.
  - [x] `mvn spring-boot:run -Dspring-boot.run.profiles=local` starts successfully on port 8088.

## Completion Summary

**All tasks completed.** Application successfully starts with:
- Spring Boot 3.4.5 + Java 21
- DashScope API integration working (embedding model invoked for 15 documents)
- Tomcat running on port 8088 with context path `/api`

Notes:
- Any credentials currently committed SHOULD be rotated outside of code changes.
- Fixed duplicate dependency declaration in pom.xml (spring-ai-mcp-client-spring-boot-starter was declared 3 times).
- Non-fatal warnings present (Nacos BeanPostProcessor, Bean Validation provider, commons-logging) - do not affect functionality.
