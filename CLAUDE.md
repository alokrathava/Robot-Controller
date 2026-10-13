# Claude Code Instructions

All AI agents working on this project, including Claude Code, MUST read and strictly follow [AGENTS.md](./AGENTS.md) before performing any development task.

`AGENTS.md` is the single source of truth for all AI development rules, scope boundaries, and design policies in this repository.

## Mandatory Directives for Claude Code:
1. **Always read `AGENTS.md` first** before starting any task.
2. **No Unrequested Tests**: Never generate unit tests, UI tests, or test fixtures unless explicitly commanded by the user.
3. **No Unrequested Documentation**: Never create READMEs, markdown guides, or docs files unless explicitly commanded.
4. **Strict Scope**: Implement ONLY the requested feature or fix. Do not refactor adjacent code or fix unrelated issues.
5. **Mandatory Comments**: Add a 1–5 line explanatory comment to every new or modified Kotlin class and function.
6. **Design System**: Always use theme tokens and components from `:core:theme` for Jetpack Compose UI changes.
