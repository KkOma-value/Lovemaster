# Tasks: fix-mcp-startup-blocking

## 1. Implementation
- [ ] Add a bean-definition post processor to lazify or remove `toolCallbacksDeprecated` and `asyncToolCallbacksDeprecated`.
- [ ] Add minimal logging so startup clearly states the mitigation is active.
- [ ] (Optional, pending confirmation) Split MCP servers config into minimal vs extended (npx) variants and document how to opt-in.
- [ ] Update default configuration to include a safer `spring.ai.mcp.client.request-timeout` value (or document recommended env var).

## 2. Validation
- [ ] Add/extend a unit test that loads a minimal Spring context and asserts startup does not instantiate `toolCallbacksDeprecated` eagerly.
- [ ] Add a regression test that simulates unavailable MCP servers (e.g., invalid command or missing jar) and asserts the application context still starts.

## 3. Documentation
- [ ] Update README / local startup notes to explain:
      - MCP is optional and non-blocking on startup
      - How to control autostart (`app.mcp.autostart`)
      - How to tune MCP timeout (`spring.ai.mcp.client.request-timeout`)
      - (If split configs) How to enable external `npx` MCP servers
