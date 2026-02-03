# Tasks: fix-manus-request-size-and-image-download

## 1. Implementation
- [x] Add request-size budgeting for Manus model calls (compute approximate length for `systemPrompt + messages` and trim safely before sending).
- [x] Add tool-output budgeting (truncate/summary + pointer) for high-volume tools: `ReadFile`, `WebScraping`, `TerminalOperation`, and any tool returning arbitrary text.
- [x] Improve image download diagnostics and robustness for:
      - local `downloadResource`
      - MCP `downloadImage`
- [x] Make `generatePDF` tolerant to non-BMP characters (emoji): sanitize/replace before writing so PDF generation doesn't fail.
- [x] Ensure chat-memory Kryo files are not polluted by plaintext logging (separate `.log` output; never append to `*.kryo`).

## 2. Validation
- [x] Add unit tests for request budget logic (given oversized history/tool output, the built request stays within the limit and retains most recent context).
- [x] Add unit tests for tool-output budgeting (oversized output is externalized and only a snippet is written back).
- [x] Extend existing PDF generation tests to cover the "downloaded image exists but path mismatch/invalid content-type" diagnostics (if applicable).

## 3. Documentation
- [x] Document the new budgeting behavior (what gets truncated, how to find full tool output on disk, and how to configure limits).
- [x] Document recommended configuration for shared `app.file-save-dir` across main app and MCP server.
