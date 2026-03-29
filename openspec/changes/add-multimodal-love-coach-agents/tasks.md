## 1. Implementation

- [ ] 1.1 Create the multimodal change design and spec deltas for screenshot intake, rewrite, and Coach routing.
- [ ] 1.2 Update the React home and chat experience to clearly separate Love mode and Coach mode, including screenshot-friendly copy.
- [ ] 1.3 Update the React chat input and streaming UI to show staged intake statuses for upload, OCR, rewrite, and answer generation.
- [ ] 1.4 Update the Coach panel behavior so ordinary analysis does not auto-open the panel, while task execution still opens it with task telemetry.
- [ ] 1.5 Extend backend chat endpoints to accept `imageUrl` and build a shared input context for Love and Coach flows.
- [ ] 1.6 Implement screenshot intake and rewrite services that can process text-only and image-assisted requests.
- [ ] 1.7 Route Love mode through intake analysis before final answer generation.
- [ ] 1.8 Route Coach mode through intake analysis and a brain-level decision before invoking Manus tool execution.
- [ ] 1.9 Add tests or validation coverage for frontend SSE handling, image-aware request wiring, and backend route decisions.
- [ ] 1.10 Run `openspec validate add-multimodal-love-coach-agents --strict`, frontend build/lint, and backend compile checks.
