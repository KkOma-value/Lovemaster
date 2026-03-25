# Super Dev Trae Rules

## Critical Trigger Switch
- If a user message starts with `super-dev:` or `super-dev：`, immediately switch into Super Dev pipeline mode.
- Do not treat `super-dev:` or `super-dev：` as normal chat, brainstorming, or generic coding mode.
- After `super-dev:` or `super-dev：` is seen, do not start implementation directly.
- Your first reply must say `SMOKE_OK` when the user is smoke-testing, or explicitly say Super Dev pipeline mode is active.
- Your first reply must explicitly say the current phase is `research`.
- Your first reply must explicitly promise the sequence: research -> three core documents -> wait for user confirmation -> Spec/tasks -> frontend runtime verification -> backend/tests/delivery.
- After the three core documents are generated, you must stop and wait for approval before creating Spec or writing code.

## Runtime Contract
- Treat Super Dev as the local Python workflow tool plus Trae rule files, not as a separate coding engine.
- Keep using the host's model, tools, browse/search/web and editor capabilities.
- Use local `super-dev` commands when you need to generate or refresh documents, specs, quality reports, or delivery manifests.
- The host remains responsible for coding, tool execution, and file changes.

## Local Knowledge Contract
- Read relevant files under `knowledge/` before drafting the three core documents.
- If `output/knowledge-cache/*-knowledge-bundle.json` exists, read it first and carry its matched local knowledge into PRD, architecture, UIUX, Spec, and execution.
- Treat matched standards, anti-patterns, checklists, baselines, and scenario packs as hard constraints.

## Working Agreement
- If browse/search/web is available, research similar products first and write `output/*-research.md` into the project workspace.
- Generate PRD, architecture, and UIUX before coding and save them as `output/*-prd.md`, `output/*-architecture.md`, and `output/*-uiux.md` instead of only replying in chat.
- Ask the user to confirm or revise the three documents before creating Spec or code.
- If a document is mentioned in chat but not written to the repository, treat the step as incomplete and keep working until the file exists.
- If the user requests a UI redesign or says the UI is unsatisfactory, first update `output/*-uiux.md`, then redo the frontend, and rerun frontend runtime + UI review before continuing.
- If the user requests architecture changes, first update `output/*-architecture.md`, then realign tasks and implementation before continuing.
- If the user requests quality or security remediation, first fix the issues, rerun quality gate plus `super-dev release proof-pack`, and only then continue.
- Implement frontend first and verify runtime before moving into backend-heavy work.
- Keep UI implementation consistent with `output/*-uiux.md` and avoid AI-looking templates.

## Super Dev System Flow Contract
- SUPER_DEV_FLOW_CONTRACT_V1
- PHASE_CHAIN: research>docs>docs_confirm>spec>frontend>preview_confirm>backend>quality>delivery
- DOC_CONFIRM_GATE: required
- PREVIEW_CONFIRM_GATE: required
- HOST_PARITY: required
