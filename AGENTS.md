# AI Agent Development Governance Policy

> [!IMPORTANT]
> **EXECUTIVE DIRECTIVES SUMMARY (MANDATORY FOR ALL AI AGENTS)**
>
> All AI agents (Claude Code, Gemini, Copilot, Cursor, Windsurf, etc.) MUST read and strictly adhere to this policy for every task in this repository:
> 1. **STRICT NO UNREQUESTED TESTS**: Do NOT generate unit, integration, UI, or snapshot tests unless explicitly commanded by the user.
> 2. **STRICT NO UNREQUESTED DOCUMENTATION**: Do NOT create READMEs, markdown files, or technical docs unless explicitly requested.
> 3. **STRICT TASK SCOPE**: Perform ONLY the exact task requested. Do not fix unrelated bugs, refactor adjacent code, or add unrequested features.
> 4. **MANDATORY CODE COMMENTING**: Every new or modified function, class, or logical block MUST include a concise explanatory comment (1 to 5 lines).
> 5. **MANDATORY DESIGN SYSTEM COMPLIANCE**: All UI changes MUST strictly reuse Jetpack Compose design tokens and components in `:core:theme` (`Color.kt`, `Theme.kt`, `Type.kt`, `Spacing.kt`, `Components.kt`, `Shapes.kt`).
> 6. **COMPLETE LIFECYCLE RESPONSIBILITY**: Manage initialization, state, error handling, cleanup, and resource disposal for all created/modified components.

---

## 1. Mandatory Agent Instructions

All AI agents MUST read and follow `AGENTS.md` before performing any development task.

`AGENTS.md` is the single source of truth for AI development rules.

- Read `AGENTS.md` before starting any task.
- Follow its instructions throughout development.
- Do not bypass or reinterpret its restrictions.
- Re-read relevant instructions when changing modules.
- If instructions conflict, prioritize explicit user instructions and mandatory safety requirements.
- Never silently ignore a rule.

Configure existing agent-specific instruction files (`CLAUDE.md`, `.github/copilot-instructions.md`, `.cursorrules`, `.windsurfrules`) to reference `AGENTS.md`. Do not duplicate the entire policy across multiple instruction files.

## 2. Strict No Automated Test Generation Policy

AI agents MUST NOT independently create automated tests.

This includes:

- Unit tests (JUnit, Mockk, etc.)
- Integration tests
- End-to-end tests
- Compose UI automation tests
- Screenshot / Snapshot tests
- Performance tests
- Test fixtures
- Mock test implementations
- Test scripts
- Automated testing infrastructure

Rules:

1. Do not generate tests unless explicitly instructed by the user.
2. Do not create test directories independently.
3. Do not install testing frameworks without authorization.
4. Do not introduce testing dependencies without authorization.
5. Do not create test-related CI/CD workflows.
6. Do not generate tests as part of refactoring.
7. Do not automatically create tests when fixing bugs.
8. Do not execute automated tests unless explicitly requested.

If the agent created unauthorized tests during its current task, it MUST delete those files.

Never delete pre-existing tests or user-authored tests without explicit authorization.

Agents may inspect code, perform static reasoning, and use non-test validation such as syntax checks and compilation, provided those actions remain within the requested task.

## 3. Documentation Policy

AI agents MUST NOT create documentation unless explicitly requested.

Prohibited without authorization:

- README files
- API documentation
- Architecture documentation
- Implementation guides
- Migration guides
- Technical reports
- Markdown documentation files
- Automatically generated documentation
- Unrequested changelogs

Exceptions:

- `AGENTS.md` and minimal agent instruction references required by this task.
- Required inline code comments.
- Documentation explicitly requested by the user.

Do not modify existing documentation unless the task requires it.

## 4. Strict Task Scope Enforcement

AI agents MUST perform ONLY the task explicitly requested.

The agent MUST NOT:

- Add unrelated features.
- Modify unrelated modules.
- Refactor unrelated code.
- Change architecture unnecessarily.
- Introduce additional dependencies without justification.
- Modify UI components outside the requested scope.
- Rename unrelated classes, functions, or files.
- Introduce speculative improvements.
- Implement future requirements without authorization.
- Modify deployment configuration without authorization.
- Perform unrelated cleanup.

If the agent identifies unrelated problems:

1. Do not fix them automatically.
2. Report them separately.
3. Continue with the original requested task.
4. Wait for authorization before making unrelated changes.

The agent must never expand the scope of a task independently.

## 5. No Overengineering

All implementation must prioritize simplicity, maintainability, and minimal complexity.

Engineering principles:

- KISS: Keep It Simple.
- YAGNI: You Aren't Gonna Need It.
- DRY: Don't Repeat Yourself, when reuse genuinely improves clarity.
- Prefer existing implementations.
- Reuse existing components in `:core:theme`, `:core:networking`, `:core:database`, and `:robot:sdk`.
- Follow established architecture (MVVM, StateFlow, Coroutines, Jetpack Compose).
- Avoid unnecessary abstractions.
- Avoid excessive design patterns.
- Avoid speculative scalability implementations.
- Avoid creating generic frameworks for one-time functionality.
- Avoid introducing new layers without a concrete requirement.

Do not create:

- Unnecessary interfaces.
- Unnecessary base classes.
- Unnecessary factories.
- Unnecessary wrappers.
- Unnecessary services.
- Unnecessary repositories.
- Unnecessary configuration systems.
- Unnecessary background workers.

Every abstraction must solve an actual requirement.

Prefer the smallest correct implementation that satisfies the requested behavior.

## 6. Mandatory Code Commenting

Every new or modified function, class, and logically independent code snippet MUST include a concise explanatory comment.

Comment requirements:

- Minimum: 1 line.
- Maximum: 5 lines.
- Explain responsibility and meaningful behavior.
- Document important assumptions where relevant.
- Explain non-obvious logic.
- Explain lifecycle responsibilities when applicable.

Rules:

1. Use Kotlin/Java standard comment format (`//` or KDoc `/** ... */`).
2. Prefer KDoc comments for public classes and functions in `:robot:sdk`.
3. Keep comments accurate when implementation changes.
4. Do not write unnecessarily verbose comments.
5. Avoid comments that merely repeat the code.
6. Do not add comments to unrelated code solely for compliance.
7. Do not use comments as a replacement for readable naming.

Example:

```kotlin
// Validates robot connectivity status and emits the updated telemetry state.
// Cancels active polling job if the network connection drops.
```

For complex functions, use up to five meaningful lines.
For short functions, one concise line is sufficient.

## 7. Complete Code Lifecycle Responsibility

Every AI agent is responsible for the complete lifecycle of the code it creates or modifies.

Do not deliver incomplete or disconnected implementations.

Every implementation must consider, where applicable:

1. Initialization
2. Configuration
3. Input validation
4. Authentication and connection state
5. Execution
6. State management (StateFlow, Compose State)
7. Error handling
8. Resource management (Coroutines Job, Socket, Flow collection)
9. Cleanup & Disposal (`ViewModel.onCleared()`, DisposableEffect)
10. Failure recovery
11. Compatibility with existing functionality

Examples:

Database (Room):

- Database access must use proper Coroutines dispatchers (`Dispatchers.IO`).
- Resource disposal and transaction safety must be preserved.

Network & SDK:

- Connections and web sockets must close appropriately.
- Timeouts and reconnect backoffs must be handled.
- Failures must not leave stale state in UI or SDK state flows.

Coroutines & ViewModel:

- Tasks must be scoped to `viewModelScope` or explicit Job lifecycles.
- Cancellation must be supported cleanly.
- Resources must be cleaned up on scope cancellation.

UI (Jetpack Compose):

- Loading, Success, Error, and Offline states must be handled cleanly.
- Event handlers and effects must release owned resources.

Complete lifecycle responsibility does not authorize additional features or unrelated architecture changes.

If a required lifecycle component is outside the authorized scope, report the dependency rather than silently expanding the implementation.

## 8. Safe Database & SDK Migrations

All migrations (Room Database, Protocol SDK, Data contracts) are considered HIGH-RISK operations.

Agents MUST prioritize runtime safety and backward compatibility.

### Migration Rules

1. Preserve existing data and offline state compatibility.
2. Never introduce breaking changes to Room database schemas without an explicit `Migration` strategy.
3. Preserve existing public API contracts in `:robot:sdk`.
4. Prefer backward-compatible API additions over breaking changes.
5. Preserve existing protocol serialization compatibility (`:core:networking` / `:robot:sdk`).

## 9. SDK & Protocol API Contract Safeguards

Public interfaces and API contracts in `:robot:sdk` and `:core:networking` MUST remain backward-compatible for existing feature modules.

API compatibility rules:

1. Do not rename or remove public SDK methods or parameters without deprecation.
2. Mark deprecated SDK methods with `@Deprecated` and specify a replacement path.
3. Maintain compatibility with existing module consumers (`:feature:home`, `:feature:manualcontrol`, `:feature:map`, `:feature:robotstatus`, `:feature:navigation`, `:feature:settings`).
4. Never break binary or source compatibility of `:robot:sdk` without explicit approval.

## 10. Change Safety

Before modifying code:

- Understand the relevant existing implementation.
- Identify affected modules across feature libraries.
- Identify potential compatibility issues.
- Check existing architectural conventions (MVI / MVVM with Compose).
- Determine the smallest safe change.

During implementation:

- Preserve existing behavior unless modification is requested.
- Do not introduce unrelated changes.
- Handle relevant failure scenarios.
- Follow established naming conventions.
- Avoid adding unnecessary dependencies.

After implementation:

- Review the changes made.
- Check for unintended modifications.
- Confirm required lifecycle handling.
- Perform permitted non-test validation (e.g., Gradle compilation, syntax checks).
- Do not create automated tests.
- Do not generate documentation.
- Do not perform unrelated cleanup.

## 11. Agent Completion Requirements

At the end of every development task, provide a concise summary.

The response must contain:

1. What was implemented.
2. Which files were modified.
3. Whether the requested functionality is complete.
4. Any important limitations or risks.

Do not generate a documentation file for this summary.
Do not claim functionality is verified unless validation was actually performed.

## 12. Strict Prohibitions

Unless explicitly requested, agents MUST NOT:

- Generate automated tests.
- Generate documentation files.
- Add unrelated functionality.
- Perform speculative refactoring.
- Introduce unnecessary architecture.
- Modify gradle build scripts outside task scope.
- Delete or break database schemas.
- Break existing SDK or protocol contracts.
- Introduce unnecessary dependencies.
- Create incomplete lifecycle implementations.
- Change unrelated code.

## 13. Mandatory Design System Compliance

All AI agents MUST follow the project's existing Jetpack Compose Design System located in `:core:theme`.

The design system in `:core:theme` is the SINGLE SOURCE OF TRUTH for all visual design, colors, typography, shapes, and component styling.

### 13.1 Design System Discovery

Before modifying any UI or Compose component, agents MUST inspect:

- `com.alokrathava.theme.Color.kt`
- `com.alokrathava.theme.Theme.kt`
- `com.alokrathava.theme.Type.kt`
- `com.alokrathava.theme.Spacing.kt`
- `com.alokrathava.theme.Shapes.kt`
- `com.alokrathava.theme.Components.kt`

### 13.2 Strict Design Token Usage

Agents MUST use existing theme design tokens via `MaterialTheme.colorScheme`, `MaterialTheme.typography`, `MaterialTheme.shapes`, or `Theme` tokens in `:core:theme`.

Agents MUST NOT:

- Hardcode raw color hex values (e.g., `Color(0xFF123456)`). Use theme color tokens.
- Hardcode raw DP/SP values where spacing tokens in `Spacing.kt` apply.
- Create duplicate theme components.
- Introduce custom fonts or typography scales outside `Type.kt`.
- Override dark/light theme tokens unnecessarily.

### 13.3 Component Reusability

Before creating a new Compose UI component:

1. Search `:core:theme` (`Components.kt`) and existing feature components.
2. Reuse existing components whenever possible.
3. Extend or parameterize existing components only when required.
4. Create a new component only when no suitable component exists in `:core:theme`.

### 13.4 Accessibility & Touch Targets

All Jetpack Compose UI implementations MUST support:

- Minimum touch target sizes (48dp for buttons and interactive controls).
- Proper content descriptions for screen readers on images and icons (`contentDescription`).
- High contrast text colors conforming to light and dark theme definitions.

## 14. Governance Maintenance Boundaries

When updating this policy:

1. Preserve core directives and project-specific instructions unless the user explicitly requests a policy change.
2. Ensure agent hook files (`CLAUDE.md`, `.github/copilot-instructions.md`, `.cursorrules`, `.windsurfrules`) reference `AGENTS.md`.
3. Do not modify application code when modifying governance files.
4. Report the changes made.
