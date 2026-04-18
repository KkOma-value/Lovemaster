# HOST_USAGE_GUIDE

## Trigger Modes
- Slash mode: /super-dev <requirement>
- Text mode: super-dev: <requirement>
- Entry mode: `super-dev start --idea "Lovemaster workflow continuation"`

## Workflow Gate Reminder
- 当前阶段是 `research`
- Keep research and document confirmation gates before implementation.

## Smoke
1. Run `super-dev status`.
2. Trigger host command with `/super-dev` or `super-dev:`.
3. Verify state files under `.super-dev/` are updated.

## Common Recovery
- Use `super-dev next` for next action.
- Use `super-dev continue` to resume current flow.
